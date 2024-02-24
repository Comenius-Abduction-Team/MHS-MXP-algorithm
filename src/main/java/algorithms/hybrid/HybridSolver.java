package algorithms.hybrid;

import algorithms.ISolver;
import com.google.common.collect.Iterables;
import common.Configuration;

import common.IPrinter;
import models.Abducibles;
import models.Explanation;
import models.Axioms;
import models.IAxioms;
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

public class HybridSolver implements ISolver {

    private ILoader loader;
    private IReasonerManager reasonerManager;
    private IAxioms abd_literals;
    private ModelExtractor modelExtractor;
    private final IExplanationManager explanationManager;
    private final IProgressManager progressManager;
    private SetDivider setDivider;
    private Set<Set<OWLAxiom>> pathsInCertainDepth = new HashSet<>();

    public OWLOntology ontology;
    public List<ModelNode> models;
    public List<ModelNode> negModels;
    public List<OWLAxiom> assertionsAxioms;
    public List<OWLAxiom> negAssertionsAxioms;
    public Set<OWLAxiom> path = new HashSet<>();
    public Set<OWLAxiom> pathDuringCheckingMinimality;
    public Abducibles abducibles;
    public int lastUsableModelIndex;
    public OWLAxiom negObservation;
    public ThreadTimes threadTimes;
    public long currentTimeMillis;
    public Map<Integer, Double> levelTimes = new HashMap<>();
    public boolean checkingMinimalityWithQXP = false;
    private IRuleChecker ruleChecker;
    private Integer currentDepth;

    public HybridSolver(ThreadTimes threadTimes,
                        IExplanationManager explanationManager, IProgressManager progressManager, IPrinter printer) {

        String info = String.join("\n", getInfo());
        printer.print("");
        printer.print(info);
        printer.print("");

        this.explanationManager = explanationManager;
        explanationManager.setSolver(this);

        this.progressManager = progressManager;

        this.threadTimes = threadTimes;
        this.currentTimeMillis = System.currentTimeMillis();
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
    public Collection<Explanation> getExplanations() {
        return explanationManager.getFinalExplanations();
    }

    @Override
    public void solve(ILoader loader, IReasonerManager reasonerManager) throws OWLOntologyStorageException, OWLOntologyCreationException {
        this.loader = loader;
        this.reasonerManager = reasonerManager;
        this.ontology = this.loader.getOriginalOntology();
        this.modelExtractor = new ModelExtractor(loader, reasonerManager, this);
        this.setDivider = new SetDivider(this);
        this.ruleChecker = new RuleChecker(loader, reasonerManager);

        negObservation = loader.getNegObservation().getOwlAxiom();
        this.abducibles = loader.getAbducibles();

        initialize();

        String message = null;

        if (!reasonerManager.isOntologyConsistent()) {
            message = "The observation is already entailed!";
            explanationManager.processExplanations(message);
        }

        else {
            reasonerManager.isOntologyWithLiteralsConsistent(abd_literals.getAxioms(), ontology);
            trySolve();
        }
        //trySolve();
        if (Configuration.PRINT_PROGRESS)
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
        levelTimes.put(currentDepth, time);
        explanationManager.logExplanationsWithDepth(currentDepth, false, true, time);
        if(!Configuration.MHS_MODE){
            explanationManager.logExplanationsWithDepth(currentDepth + 1, false, true, time);
            explanationManager.logExplanationsWithLevel(currentDepth, false, true, time);
        }
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

    private void startSolving() throws OWLOntologyCreationException {
        if (Configuration.PRINT_PROGRESS)
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

            //System.out.println("model = " + model.data);
            //System.out.println(model.data.size());

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
                    if(Configuration.MHS_MODE){
                        if(!isOntologyConsistent()){
                            explanation.setDepth(explanation.getOwlAxioms().size());
                            explanationManager.addPossibleExplanation(explanation);
                            path.clear();
                            continue;
                        }
                    } else {
                        if (!addNewExplanations()){
                            path.clear();
                            if(isTimeout()){
                                makeTimeoutPartialLog();
                                return;
                            }
                            continue;
                        }
                        if(isTimeout()){
                            makeTimeoutPartialLog();
                            return;
                        }
                    }
                }
                else{
                    explanationManager.setLengthOneExplanations(new ArrayList<>());
                }
                addNodeToTree(queue, explanation, model);
            }
        }
        path.clear();

        if(!levelTimes.containsKey(currentDepth)){
            makePartialLog();
        }
        currentDepth = 0;
    }

    private void makePartialLog() {
        Double time = threadTimes.getTotalUserTimeInSec();
        levelTimes.put(currentDepth, time);
        explanationManager.logExplanationsWithDepth(currentDepth, false, false, time);
        if(!Configuration.MHS_MODE){
            explanationManager.logExplanationsWithLevel(currentDepth, false, false, time);
        }
        pathsInCertainDepth = new HashSet<>();
    }

    private void initializeTree(Queue<TreeNode> queue) {
        if(Configuration.MHS_MODE){
            if(!isOntologyConsistent()){
                return;
            }
        } else {
            Conflict conflict = getMergeConflict();
            for (Explanation e: conflict.getExplanations()){
                e.setDepth(e.getOwlAxioms().size());
            }
            explanationManager.setPossibleExplanations(conflict.getExplanations());
        }

        ModelNode root = createModelNodeFromExistingModel(true, null, null);
        if(root == null){
            return;
        }
        queue.add(root);
    }

    protected void addToExplanations(Explanation explanation){
        explanation.setDepth(explanation.getOwlAxioms().size());
        if(Configuration.CHECKING_MINIMALITY_BY_QXP){
            Explanation newExplanation = getMinimalExplanationByCallingQXP(explanation);
            explanationManager.addPossibleExplanation(newExplanation);
            if(Configuration.CACHED_CONFLICTS_MEDIAN){
                setDivider.addPairsOfLiteralsToTable(newExplanation);
            }
        } else {
            explanationManager.addPossibleExplanation(explanation);
            if(Configuration.CACHED_CONFLICTS_MEDIAN){
                setDivider.addPairsOfLiteralsToTable(explanation);
            }
        }
    }

    private void addNodeToTree(Queue<TreeNode> queue, Explanation explanation, ModelNode model){
        ModelNode modelNode = createModelNodeFromExistingModel(false, explanation, model.depth + 1);
        if(modelNode == null){
            path.clear();
            return;
        }
        if(!Configuration.MHS_MODE){
            modelNode.addLengthOneExplanationsFromNode(model);
            modelNode.addLengthOneExplanations(explanationManager.getLengthOneExplanations());
        }
        queue.add(modelNode);
        path.clear();
    }

    private ModelNode createModelNodeFromExistingModel(boolean isRoot, Explanation explanation, Integer depth){
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

    private boolean increaseDepth(TreeNode node){
        if (node.depth > currentDepth){
            makePartialLog();
            if (Configuration.PRINT_PROGRESS)
                progressManager.updateProgress(currentDepth, threadTimes.getTotalUserTimeInSec());
            return true;
        }
        return false;
    }

    private boolean isTimeout(){
        if (Configuration.TIMEOUT != null && threadTimes.getTotalUserTimeInSec() > Configuration.TIMEOUT) {
            return true;
        }
        return false;
    }

    private void makeTimeoutPartialLog() {
        Double time = threadTimes.getTotalUserTimeInSec();
        levelTimes.put(currentDepth, time);
        explanationManager.logExplanationsWithDepth(currentDepth, true, false, time);
        if(!Configuration.MHS_MODE){
            explanationManager.logExplanationsWithDepth(currentDepth + 1, true, false, time);
            explanationManager.logExplanationsWithLevel(currentDepth, true,false, time);
        }
    }

    private boolean canBePruned(Explanation explanation) throws OWLOntologyCreationException {
        if (!ruleChecker.isMinimal(explanationManager.getPossibleExplanations(), explanation)){
            return true;
        }
        if(pathsInCertainDepth.contains(path)){
            return true;
        }
        pathsInCertainDepth.add(new HashSet<>(path));

        if(Configuration.CHECK_RELEVANCE_DURING_BUILDING_TREE_IN_MHS_MXP){
            if(!ruleChecker.isRelevant(explanation)){
                return true;
            }
        }

        if(Configuration.MHS_MODE){
            if(!ruleChecker.isRelevant(explanation)){
                return true;
            }
            if(!ruleChecker.isConsistent(explanation)){
                return true;
            }
        }

        if(!Configuration.MHS_MODE){
            if (ruleChecker.isExplanation(explanation)){
                addToExplanations(explanation);
                return true;
            }
        }
        return false;
    }

    private boolean isIncorrectPath(ModelNode model, OWLAxiom child){
        if (model.label.contains(AxiomManager.getComplementOfOWLAxiom(loader, child)) ||
                child.equals(loader.getObservation().getOwlAxiom())){
            return true;
        }

        if (!abd_literals.getAxioms().contains(child)){
            //System.out.println(child + "literal not in abducibles");
            return true;
        }

        return false;
    }

    Conflict getMergeConflict() {
        return findConflicts(abd_literals);
    }

    private List<Explanation> findExplanations(){
        abd_literals.removeAll(path);
        abd_literals.removeAll(explanationManager.getLengthOneExplanations());
        if(Configuration.CACHED_CONFLICTS_LONGEST_CONFLICT){
            setDivider.setIndexesOfExplanations(explanationManager.getPossibleExplanationsCount());
        }
        Conflict conflict = findConflicts(abd_literals);
        abd_literals.addAll(path);
        abd_literals.addAll(explanationManager.getLengthOneExplanations());
        return conflict.getExplanations();
    }

    private Conflict findConflicts(IAxioms literals) {
        path.remove(negObservation);
        reasonerManager.addAxiomsToOntology(path);

        if (isTimeout()) {
            return new Conflict(new Axioms(), new LinkedList<>());
        }

        if (isOntologyWithLiteralsConsistent(literals.getAxioms())) {
            return new Conflict(literals, new LinkedList<>());
        }
        resetOntologyToOriginal();
        if (literals.getAxioms().size() == 1) {
            List<Explanation> explanations = new LinkedList<>();
            explanations.add(new Explanation(literals.getAxioms(), literals.getAxioms().size(), currentDepth, threadTimes.getTotalUserTimeInSec()));
            return new Conflict(new Axioms(), explanations);
        }

        int indexOfExplanation = -1;
        if(Configuration.CACHED_CONFLICTS_LONGEST_CONFLICT){
            indexOfExplanation = setDivider.getIndexOfTheLongestAndNotUsedConflict();
        }

        List<Axioms> sets = setDivider.divideIntoSets(literals);
        double median = setDivider.getMedian();

        Conflict conflictC1 = findConflicts(sets.get(0));
        if(Configuration.CACHED_CONFLICTS_LONGEST_CONFLICT){
            setDivider.addIndexToIndexesOfExplanations(indexOfExplanation);
        } else if(Configuration.CACHED_CONFLICTS_MEDIAN){
            setDivider.setMedian(median);
        }

        Conflict conflictC2 = findConflicts(sets.get(1));
        if(Configuration.CACHED_CONFLICTS_LONGEST_CONFLICT){
            setDivider.addIndexToIndexesOfExplanations(indexOfExplanation);
        } else if(Configuration.CACHED_CONFLICTS_MEDIAN){
            setDivider.setMedian(median);
        }

        List<Explanation> explanations = new LinkedList<>();
        explanations.addAll(conflictC1.getExplanations());
        explanations.addAll(conflictC2.getExplanations());

        Axioms conflictLiterals = new Axioms();
        conflictLiterals.addAll(conflictC1.getAxioms().getAxioms());
        conflictLiterals.addAll(conflictC2.getAxioms().getAxioms());

        while (!isOntologyWithLiteralsConsistent(conflictLiterals.getAxioms())) {

            if ((Configuration.DEPTH == null || Configuration.DEPTH == 0 || Configuration.DEPTH == Integer.MAX_VALUE) && Configuration.TIMEOUT != null)
                if (Configuration.PRINT_PROGRESS)
                    progressManager.updateProgress(currentDepth, threadTimes.getTotalUserTimeInSec());

            if (isTimeout()) break;

            path.addAll(conflictC2.getAxioms().getAxioms());
            Explanation X = getConflict(conflictC2.getAxioms().getAxioms(), conflictC1.getAxioms(), path);
            path.removeAll(conflictC2.getAxioms().getAxioms());

            path.addAll(X.getOwlAxioms());
            Explanation CS = getConflict(X.getOwlAxioms(), conflictC2.getAxioms(), path);
            path.removeAll(X.getOwlAxioms());

            CS.getOwlAxioms().addAll(X.getOwlAxioms());

            conflictLiterals.removeAll(conflictC1.getAxioms().getAxioms());
            X.getOwlAxioms().stream().findFirst().ifPresent(axiom -> conflictC1.getAxioms().remove(axiom));
            conflictLiterals.addAll(conflictC1.getAxioms().getAxioms());

            if (explanations.contains(CS) || isTimeout()) {
                break;
            }

            Explanation newExplanation = CS;
            if(Configuration.CHECKING_MINIMALITY_BY_QXP){
                newExplanation = getMinimalExplanationByCallingQXP(CS);
            }
            explanations.add(newExplanation);
            if(Configuration.CACHED_CONFLICTS_MEDIAN){
                setDivider.addPairsOfLiteralsToTable(newExplanation);
            }
        }

        return new Conflict(conflictLiterals, explanations);
    }

    private Explanation getConflict(Collection<OWLAxiom> axioms, IAxioms literals, Set<OWLAxiom> actualPath) {

        if (isTimeout()) {
            return new Explanation();
        }

        if (!axioms.isEmpty() && !isOntologyConsistent()) {
            return new Explanation();
        }

        if (literals.getAxioms().size() == 1) {
            return new Explanation(literals.getAxioms(), 1, currentDepth, threadTimes.getTotalUserTimeInSec());
        }

        List<Axioms> sets = setDivider.divideIntoSetsWithoutCondition(literals);

        actualPath.addAll(sets.get(0).getAxioms());
        Explanation D2 = getConflict(sets.get(0).getAxioms(), sets.get(1), actualPath);
        actualPath.removeAll(sets.get(0).getAxioms());

        actualPath.addAll(D2.getOwlAxioms());
        Explanation D1 = getConflict(D2.getOwlAxioms(), sets.get(0), actualPath);
        actualPath.removeAll(D2.getOwlAxioms());

        Set<OWLAxiom> conflicts = new HashSet<>();
        conflicts.addAll(D1.getOwlAxioms());
        conflicts.addAll(D2.getOwlAxioms());

        return new Explanation(conflicts, conflicts.size(), currentDepth, threadTimes.getTotalUserTimeInSec());
    }

    private boolean usableModelInModels(){
        for (int i = models.size()-1; i >= 0 ; i--){
            if (models.get(i).data.containsAll(path)){
                lastUsableModelIndex = i;
                return true;
            }
        }
        return false;
    }

    private boolean addNewExplanations(){
        List<Explanation> newExplanations = findExplanations();
        explanationManager.setLengthOneExplanations(new ArrayList<>());
        for (Explanation conflict : newExplanations){
            if (conflict.getOwlAxioms().size() == 1){
                //explanationManager.addLengthOneExplanation(conflict.getOwlAxioms().stream().findFirst().orElse(null));
                explanationManager.addLengthOneExplanation(Iterables.get(conflict.getOwlAxioms(), 0));
            }
            conflict.addAxioms(path);
            if (ruleChecker.isMinimal(explanationManager.getPossibleExplanations(), conflict)){
                Explanation newExplanation = conflict;
                if(Configuration.CHECKING_MINIMALITY_BY_QXP){
                    newExplanation = getMinimalExplanationByCallingQXP(conflict);
                }
                newExplanation.setDepth(newExplanation.getOwlAxioms().size());
                explanationManager.addPossibleExplanation(newExplanation);
                if(Configuration.CACHED_CONFLICTS_MEDIAN){
                    setDivider.addPairsOfLiteralsToTable(newExplanation);
                }
            }
        }
        if (newExplanations.size() == explanationManager.getLengthOneExplanationsCount()){
            return false;
        }
        return !newExplanations.isEmpty();
    }

    private boolean isOntologyWithLiteralsConsistent(Collection<OWLAxiom> axioms){
        path.addAll(axioms);
        boolean isConsistent = isOntologyConsistent();
        path.removeAll(axioms);
        return isConsistent;
    }

    private boolean isOntologyConsistent(){
        ModelNode node = modelExtractor.getNegModelByOntology();
        return node.modelIsValid;
    }

    public Explanation getMinimalExplanationByCallingQXP(Explanation explanation){
        Set<OWLAxiom> temp = new HashSet<>(explanation.getOwlAxioms());
        if(path != null){
            temp.addAll(path);
        }
        Axioms potentialExplanations = new Axioms(temp);

        checkingMinimalityWithQXP = true;
        pathDuringCheckingMinimality = new HashSet<>();
        Explanation newExplanation = getConflict(new ArrayList<>(), potentialExplanations, pathDuringCheckingMinimality);
        checkingMinimalityWithQXP = false;
        pathDuringCheckingMinimality = new HashSet<>();

        return newExplanation;
    }

    public void resetOntologyToOriginal(){
        reasonerManager.resetOntology(loader.getOriginalOntology().axioms());
    }

}