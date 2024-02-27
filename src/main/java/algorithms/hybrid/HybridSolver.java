package algorithms.hybrid;

import algorithms.ISolver;
import com.google.common.collect.Iterables;
import common.Configuration;

import common.IPrinter;
import models.*;
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
    private IAxioms abducibleAxioms;
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
    private int globalMin;

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

        if (Configuration.HST){
            //Initially, MIN is set to |COMP|
            globalMin = abducibleAxioms.getAxioms().size();
            //System.out.println("GLOBAL MIN INIT VALUE: " + globalMin);
        }

        String message;

        if (!reasonerManager.isOntologyConsistent()) {
            message = "The observation is already entailed!";
            explanationManager.processExplanations(message);
        }

        else {
            reasonerManager.isOntologyWithLiteralsConsistent(abducibleAxioms.getAxioms(), ontology);
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
            message = "An error occured: " + e.getMessage();
            throw e;
        } finally {
            explanationManager.processExplanations(message);
        }
    }

    private void makeErrorAndPartialLog(Throwable e) {
        explanationManager.logError(e);

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

            if (Configuration.HST)
                abducibleAxioms = new NumberedAxioms(abduciblesWithoutObservation);
            else
                abducibleAxioms = new Axioms(abduciblesWithoutObservation);

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

        Set<OWLAxiom> abduciblesToAdd = new HashSet<>(assertionsAxioms);

        if(Configuration.NEGATION_ALLOWED)
            abduciblesToAdd.addAll(negAssertionsAxioms);

        if (Configuration.HST)
            abducibleAxioms = new NumberedAxioms(abduciblesToAdd);
        else
            abducibleAxioms = new Axioms(abduciblesToAdd);
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
            if(isTimeout()){
                makeTimeoutPartialLog();
                break;
            }

            ModelNode model = (ModelNode) node;

            if (Configuration.DEBUG_PRINT)
                System.out.println("*********\n" + "PROCESSING child of " + model.parentIndex + ": " + model.index + ". " + model.data);

            if (model.depth.equals(Configuration.DEPTH)) {
                break;
            }

            NumberedAxioms abducibles = null;

            if (Configuration.HST){

                abducibles = (NumberedAxioms) abducibleAxioms;

                for (OWLAxiom child : model.data){
                    //For every component C in y with no previously defined index ci,
                    // let ci(C) be MIN and decrement MIN afterwards.
                    if (abducibles.contains(child) && NumberedAxioms.DEFAULT_INDEX.equals(abducibles.getIndex(child))){
                        if (globalMin > 0){
                            abducibles.setIndex(child,globalMin);
                            globalMin -= 1;
                        }
                    }
                }
                //Let min(v) be MIN + 1
                model.min = globalMin + 1;
                if (Configuration.DEBUG_PRINT)
                    System.out.println("min of " + model.index + " == " + model.min);

                // If i(v) > min(v) create a new array ranging over min(v), . . . , i(v)−1.
                // Otherwise, let mark(v) = × and create no child nodes for v.

                if (model.index <= model.min){
                    if (Configuration.DEBUG_PRINT)
                        System.out.println(model.index + " <= " + model.min);
                    continue;
                }

            }

            //for (OWLAxiom child : model.data){
            for (OWLAxiom child : abducibleAxioms.getAxioms()){

                Integer index = NumberedAxioms.DEFAULT_INDEX;

                if (Configuration.DEBUG_PRINT)
                    System.out.println("TRYING EDGE: " + abducibles.getIndex(child) + ". " + child);

                if (Configuration.HST){
                    index = abducibles.getIndex(child);
                    //child not in abducibles
                    if (index == null)
                        continue;
                    //a new array ranging over min(v), . . . , i(v)−1
                    if (index < model.min)
                        continue;
                    if (index >= model.index)
                        continue;
                    if (index < 1)
                        continue;
                }

                if(isTimeout()){
                    makeTimeoutPartialLog();
                    return;
                }

                //ak je axiom negaciou axiomu na ceste k vrcholu, alebo
                //ak axiom nie je v abducibles
                //nepokracujeme vo vetve
                if(isIncorrectPath(model, child)){
                    if (Configuration.DEBUG_PRINT)
                        System.out.println("INCORRECT PATH: " + abducibles.getIndex(child));
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
                    if (Configuration.HST && Configuration.DEBUG_PRINT){
                        System.out.println("CAN BE PRUNED: " + abducibles.getIndex(child));
                    }
                    path.clear();
                    continue;
                }

//                if (Configuration.HST && Configuration.DEBUG_PRINT){
//                    //System.out.println("PARENT: " + model.index + " | MIN: " + model.min + " | EDGE: " + abducibles.getIndex(child) + ". " + child);
//                    System.out.println("child will be created");
//                }

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
                            if (Configuration.DEBUG_PRINT)
                                System.out.println("not add new explanations?");
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

                addNodeToTree(queue, explanation, model, index);
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
        if (Configuration.HST){
            //Set i(v) = |COMP| + 1
            root.index = abducibleAxioms.getAxioms().size() + 1;
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

    private void addNodeToTree(Queue<TreeNode> queue, Explanation explanation, ModelNode parent, int index){
        ModelNode newNode = createModelNodeFromExistingModel(false, explanation, parent.depth + 1);
        if(newNode == null){
            path.clear();
            return;
        }
        if(!Configuration.MHS_MODE){
            newNode.addLengthOneExplanationsFromNode(parent);
            newNode.addLengthOneExplanations(explanationManager.getLengthOneExplanations());
        }
        if (Configuration.HST){
            newNode.index = index;
            newNode.parentIndex = parent.index;
        }
        queue.add(newNode);
        if (Configuration.HST && Configuration.DEBUG_PRINT){
            System.out.println("Created node: " + index + ", child of " + parent.index);
        }
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

        if (!Configuration.HST && !abducibleAxioms.getAxioms().contains(child)){
            //System.out.println(child + "literal not in abducibles");
            return true;
        }

        return false;
    }

    Conflict getMergeConflict() {
        return findConflicts(abducibleAxioms);
    }

    private List<Explanation> findExplanations(){
        IAxioms backup = abducibleAxioms.copy();
        abducibleAxioms.removeAll(path);
        abducibleAxioms.removeAll(explanationManager.getLengthOneExplanations());
        if(Configuration.CACHED_CONFLICTS_LONGEST_CONFLICT){
            setDivider.setIndexesOfExplanations(explanationManager.getPossibleExplanationsCount());
        }
        Conflict conflict = findConflicts(abducibleAxioms);
        abducibleAxioms = backup;
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