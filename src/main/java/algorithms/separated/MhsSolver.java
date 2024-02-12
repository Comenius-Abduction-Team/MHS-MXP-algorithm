package algorithms.separated;

import algorithms.hybrid.*;
import common.Configuration;

import common.IPrinter;
import models.Abducibles;
import models.Explanation;
import models.Axioms;
import org.semanticweb.owlapi.model.*;

import progress.IProgressManager;
import reasoner.AxiomManager;
import reasoner.ILoader;
import reasoner.IReasonerManager;
import timer.ThreadTimes;

import java.util.*;

/**
 * Base = knowledgeBase + negObservation
 * Literals = set of all literals / concepts with named individual except observation
 */

public class MhsSolver extends HybridSolver {

    private ILoader loader;
    protected IReasonerManager reasonerManager;
    protected Axioms abd_literals;
    protected ModelExtractor modelExtractor;
    final IExplanationManager explanationManager;
    protected final IProgressManager progressManager;
    protected Set<Set<OWLAxiom>> pathsInCertainDepth = new HashSet<>();

    public OWLOntology ontology;
    public List<ModelNode> models;
    public List<ModelNode> negModels;
    public List<OWLAxiom> assertionsAxioms;
    public List<OWLAxiom> negAssertionsAxioms;
    public Set<OWLAxiom> path = new HashSet<>();
    public Abducibles abducibles;
    public int lastUsableModelIndex;
    public OWLAxiom negObservation;
    public ThreadTimes threadTimes;
    public long currentTimeMillis;
    public Map<Integer, Double> level_times = new HashMap<>();
    protected IRuleChecker checkRules;
    protected Integer currentDepth;

    public MhsSolver(ThreadTimes threadTimes, long currentTimeMillis,
                        IExplanationManager explanationManager, IProgressManager progressManager, IPrinter printer) {

        super(threadTimes, explanationManager, progressManager, printer);

        String info = String.join("\n", getInfo());
        printer.print("");
        printer.print(info);
        printer.print("");

        this.explanationManager = explanationManager;
        explanationManager.setSolver(this);

        this.progressManager = progressManager;

        this.threadTimes = threadTimes;
        this.currentTimeMillis = currentTimeMillis;
    }

    public IExplanationManager getExplanationManager(){
        return explanationManager;
    }

    public List<String> getInfo() {
        String optimizationQXP = "Optimization QXP: " + Configuration.CHECKING_MINIMALITY_BY_QXP;
        String optimizationLongestConf = "Optimization Cached Conflicts - The Longest Conflict: " + Configuration.CACHED_CONFLICTS_LONGEST_CONFLICT;
        String optimizationMedian = "Optimization Cached Conflicts - Median: " + Configuration.CACHED_CONFLICTS_MEDIAN;
        String roles = "Roles: " + Configuration.ROLES_IN_EXPLANATIONS_ALLOWED;
        String looping = "Looping allowed: " + Configuration.LOOPING_ALLOWED;
        String negation = "Negation: " +  Configuration.NEGATION_ALLOWED;
        String mhs_mode = "MHS MODE: " + Configuration.MHS_MODE;
        String relevance = "Strict relevance: " + Configuration.STRICT_RELEVANCE;
        String depth = "Depth limit: ";
        if (Configuration.DEPTH != null) depth += Configuration.DEPTH; else depth += "none";
        String timeout = "Timeout: ";
        if (Configuration.TIMEOUT != null) timeout += Configuration.TIMEOUT; else timeout += "none";

        return Arrays.asList(optimizationQXP, optimizationLongestConf, optimizationMedian,
                roles, looping, negation, mhs_mode, relevance, depth, timeout);
    }

    @Override
    public void solve(ILoader loader, IReasonerManager reasonerManager) throws OWLOntologyStorageException, OWLOntologyCreationException {
        this.loader = loader;
        this.reasonerManager = reasonerManager;
        this.ontology = this.loader.getOriginalOntology();
        this.modelExtractor = new ModelExtractor(loader, reasonerManager, this);
        this.checkRules = new RuleChecker(loader, reasonerManager);

        negObservation = loader.getNegObservation().getOwlAxiom();
        this.abducibles = loader.getAbducibles();

        initialize();

        String message = null;

        if (!reasonerManager.isOntologyConsistent()) {
            message = "The observation is already entailed!";
            explanationManager.processExplanations(message);
        }

        else {
            reasonerManager.isOntologyWithLiteralsConsistent(abd_literals.getAxiomSet(), ontology);
            trySolve();
        }

        progressManager.updateProgress(100, "Abduction finished.");
    }

    private void trySolve() throws OWLOntologyStorageException, OWLOntologyCreationException {
        String message = null;
        try {
            startSolving();
        } catch (Throwable e) {
            makeErrorAndPartialLog(e);
            message = "An error occured!";
            throw e;
        } finally {
            explanationManager.processExplanations(message);
        }
    }

    private void makeErrorAndPartialLog(Throwable e) {
        explanationManager.showError(e);

        Double time = threadTimes.getTotalUserTimeInSec();
        level_times.put(currentDepth, time);
        explanationManager.logExplanationsWithDepth(currentDepth, false, true, time);
    }

    private void initialize() {
        models = new ArrayList<>();
        negModels = new ArrayList<>();

        assertionsAxioms = new ArrayList<>();
        negAssertionsAxioms = new ArrayList<>();

        loader.getOntologyManager().addAxiom(ontology, loader.getNegObservation().getOwlAxiom());
        reasonerManager.addAxiomToOntology(loader.getNegObservation().getOwlAxiom());

        if(loader.isAxiomBasedAbduciblesOnInput()){
            Set<OWLAxiom> abduciblesWithoutObservation = abducibles.getAxiomBasedAbducibles();
            if (loader.isMultipleObservationOnInput()){
                if (Configuration.STRICT_RELEVANCE) {
                    loader.getObservation().getAxiomsInMultipleObservations().forEach(abduciblesWithoutObservation::remove);
                }
            } else {
                abduciblesWithoutObservation.remove(loader.getObservation().getOwlAxiom());
            }
            abd_literals = new Axioms(abduciblesWithoutObservation);
            return;
        }

        for(OWLClass owlClass : abducibles.getClasses()){
            if (owlClass.isTopEntity() || owlClass.isBottomEntity()) continue;
            List<OWLAxiom> classAssertionAxiom = AxiomManager.createClassAssertionAxiom(loader, owlClass);
            for (int i = 0; i < classAssertionAxiom.size(); i++) {
                if (i % 2 == 0) {
                    assertionsAxioms.add(classAssertionAxiom.get(i));
                } else {
                    negAssertionsAxioms.add(classAssertionAxiom.get(i));
                }
            }
        }

        if(Configuration.ROLES_IN_EXPLANATIONS_ALLOWED){
            for(OWLObjectProperty objectProperty : abducibles.getRoles()){
                if (objectProperty.isTopEntity() || objectProperty.isBottomEntity()) continue;
                List<OWLAxiom> objectPropertyAssertionAxiom = AxiomManager.createObjectPropertyAssertionAxiom(loader, objectProperty);
                for (int i = 0; i < objectPropertyAssertionAxiom.size(); i++) {
                    if (i % 2 == 0) {
                        assertionsAxioms.add(objectPropertyAssertionAxiom.get(i));
                    } else {
                        negAssertionsAxioms.add(objectPropertyAssertionAxiom.get(i));
                    }
                }
            }
        }

        if (loader.isMultipleObservationOnInput()){
            if (Configuration.STRICT_RELEVANCE) {
                assertionsAxioms.removeAll(loader.getObservation().getAxiomsInMultipleObservations());
                negAssertionsAxioms.removeAll(loader.getObservation().getAxiomsInMultipleObservations());
            }
        } else {
            assertionsAxioms.remove(loader.getObservation().getOwlAxiom());
            negAssertionsAxioms.remove(loader.getObservation().getOwlAxiom());
        }

        Set<OWLAxiom> to_abd = new HashSet<>();

        if(Configuration.NEGATION_ALLOWED){
            to_abd.addAll(assertionsAxioms);
            to_abd.addAll(negAssertionsAxioms);
        } else {
            to_abd.addAll(assertionsAxioms);
        }

        abd_literals = new Axioms(to_abd);
    }

    protected void startSolving() throws OWLOntologyCreationException {
        progressManager.updateProgress(0, "Abduction initialized.");
        currentDepth = 0;

        Queue<TreeNode> queue = new LinkedList<>();
        initializeTree(queue);

        if(isTimeout()) {
            makeTimeoutPartialLog();
            return;
        }

        while (!queue.isEmpty()) {
            TreeNode node = queue.poll();

            if(increaseDepth(node)){
                currentDepth++;
            }
            if(isTimeout() || !ModelNode.class.isAssignableFrom(node.getClass())){
                makeTimeoutPartialLog();
                break;
            }

            ModelNode model = (ModelNode) node;
            if (model.depth.equals(Configuration.DEPTH)) {
                break;
            }

            for (OWLAxiom child : model.data){

                if(isTimeout()){
                    makeTimeoutPartialLog();
                    return;
                }

                //ak je axiom negaciou axiomu na ceste k vrcholu, alebo
                //ak axiom nie je v abducibles
                //nepokracujeme vo vetve
                if(isIncorrectPath(model, child)){
                    continue;
                }

                //rovno pridame potencialne vysvetlenie
                Explanation explanation = new Explanation();
                explanation.addAxioms(model.label);
                explanation.addAxiom(child);
                explanation.setAcquireTime(threadTimes.getTotalUserTimeInSec());
                explanation.setLevel(currentDepth);

                path = new HashSet<>(explanation.getOwlAxioms());

                if(canBePruned(explanation)){
                    path.clear();
                    continue;
                }

                if (!Configuration.REUSE_OF_MODELS || !usableModelInModels()) {
                    if(isTimeout()){
                        makeTimeoutPartialLog();
                        return;
                    }
                    if(!isOntologyConsistent()){
                        explanation.setDepth(explanation.getOwlAxioms().size());
                        explanationManager.addPossibleExplanation(explanation);
                        path.clear();
                        continue;
                    }
                }
                else{
                    explanationManager.setLengthOneExplanations(new ArrayList<>());
                }
                addNodeToTree(queue, explanation, model);
            }
        }
        path.clear();

        if(!level_times.containsKey(currentDepth)){
            makePartialLog();
        }
        currentDepth = 0;
    }

    protected void makePartialLog() {
        Double time = threadTimes.getTotalUserTimeInSec();
        level_times.put(currentDepth, time);
        explanationManager.logExplanationsWithDepth(currentDepth, false, false, time);
        pathsInCertainDepth = new HashSet<>();
    }

    protected void initializeTree(Queue<TreeNode> queue) {

        if(isOntologyConsistent()){
            initializeRoot(queue);
        }

    }

    protected void initializeRoot(Queue<TreeNode> queue){
        ModelNode root = createModelNodeFromExistingModel(true, null, null);
        if(root == null){
            return;
        }
        queue.add(root);
    }

    protected void addNodeToTree(Queue<TreeNode> queue, Explanation explanation, ModelNode model){
        ModelNode modelNode = createModelNodeFromExistingModel(false, explanation, model.depth + 1);
        if(modelNode == null){
            path.clear();
            return;
        }
        queue.add(modelNode);
        path.clear();
    }

    protected ModelNode createModelNodeFromExistingModel(boolean isRoot, Explanation explanation, Integer depth){
        ModelNode modelNode = new ModelNode();
        if (usableModelInModels()){
            if(isRoot){
                modelNode.data = negModels.get(lastUsableModelIndex).data;
                modelNode.label = new LinkedList<>();
                modelNode.depth = 0;
            } else {
                modelNode.label = explanation.getOwlAxioms();
                modelNode.data = negModels.get(lastUsableModelIndex).data;
                modelNode.data.removeAll(path);
                modelNode.depth = depth;
            }
        }
        if(modelNode.data == null || modelNode.data.isEmpty()){
            return null;
        }
        return modelNode;
    }

    protected boolean increaseDepth(TreeNode node){
        if (node.depth > currentDepth){
            makePartialLog();
            progressManager.updateProgress(currentDepth, threadTimes.getTotalUserTimeInSec());
            return true;
        }
        return false;
    }

    protected boolean isTimeout(){
        if (Configuration.TIMEOUT != null && threadTimes.getTotalUserTimeInSec() > Configuration.TIMEOUT) {
            return true;
        }
        return false;
    }

    protected void makeTimeoutPartialLog() {
        Double time = threadTimes.getTotalUserTimeInSec();
        level_times.put(currentDepth, time);
        explanationManager.logExplanationsWithDepth(currentDepth, true, false, time);
    }

    protected boolean canBePruned(Explanation explanation) throws OWLOntologyCreationException {
        if (!checkRules.isMinimal(explanationManager.getPossibleExplanations(), explanation)){
            return true;
        }
        if(pathsInCertainDepth.contains(path)){
            return true;
        }
        pathsInCertainDepth.add(new HashSet<>(path));

        if(Configuration.CHECK_RELEVANCE_DURING_BUILDING_TREE_IN_MHS_MXP){ //suvisi aj s MHS alebo cisto s MHS-MXP???
            if(!checkRules.isRelevant(explanation)){
                return true;
            }
        }

        if(!checkRules.isRelevant(explanation)){
            return true;
        }
        if(!checkRules.isConsistent(explanation)){
            return true;
        }

        return false;
    }

    protected boolean isIncorrectPath(ModelNode model, OWLAxiom child){
        if (model.label.contains(AxiomManager.getComplementOfOWLAxiom(loader, child)) ||
                child.equals(loader.getObservation().getOwlAxiom())){
            return true;
        }

        if (!abd_literals.contains(child)){
            return true;
        }

        return false;
    }

    protected boolean usableModelInModels(){
        for (int i = models.size()-1; i >= 0 ; i--){
            if (models.get(i).data.containsAll(path)){
                lastUsableModelIndex = i;
                return true;
            }
        }
        return false;
    }

    private boolean isOntologyConsistent(){
        ModelNode node = modelExtractor.getNegModelByOntology();
        return node.modelIsValid;
    }

    public void resetOntologyToOriginal(){
        reasonerManager.resetOntology(loader.getOriginalOntology().axioms());
    }

}