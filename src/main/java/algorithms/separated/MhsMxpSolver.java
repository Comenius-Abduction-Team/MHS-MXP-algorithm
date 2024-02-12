package algorithms.separated;

import algorithms.hybrid.*;
import com.google.common.collect.Iterables;
import common.Configuration;
import common.IPrinter;
import models.Explanation;
import models.Axioms;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import progress.IProgressManager;
import timer.ThreadTimes;

import java.util.*;

public class MhsMxpSolver extends MhsSolver{

    private final SetDivider setDivider;

    public MhsMxpSolver(ThreadTimes threadTimes, long currentTimeMillis,
                        IExplanationManager explanationManager, IProgressManager progressManager, IPrinter printer) {

        super(threadTimes, currentTimeMillis, explanationManager, progressManager, printer);
        setDivider = new SetDivider(this);
    }

    @Override
    protected void initializeTree(Queue<TreeNode> queue){
        Conflict conflict = getMergeConflict();
        List<Explanation> explanations = conflict.getExplanations();
        for (Explanation e : explanations){
            e.setDepth(e.getOwlAxioms().size());
        }
        explanationManager.setPossibleExplanations(explanations);
    }

    @Override
    protected void addNodeToTree(Queue<TreeNode> queue, Explanation explanation, ModelNode model){
        ModelNode modelNode = createModelNodeFromExistingModel(false, explanation, model.depth + 1);
        if(modelNode == null){
            path.clear();
            return;
        }
        ////////////////////////////
        modelNode.addLengthOneExplanationsFromNode(model);
        modelNode.addLengthOneExplanations(explanationManager.getLengthOneExplanations());
        ////////////////////////////
        queue.add(modelNode);
        path.clear();
    }

    @Override
    protected void makePartialLog() {
        Double time = threadTimes.getTotalUserTimeInSec();
        level_times.put(currentDepth, time);
        explanationManager.logExplanationsWithDepth(currentDepth, false, false, time);
        explanationManager.logExplanationsWithLevel(currentDepth, false, false, time);
        pathsInCertainDepth = new HashSet<>();
    }

    @Override
    protected void makeTimeoutPartialLog() {
        Double time = threadTimes.getTotalUserTimeInSec();
        level_times.put(currentDepth, time);
        explanationManager.logExplanationsWithDepth(currentDepth, true, false, time);
        ////////////////////////////
        explanationManager.logExplanationsWithDepth(currentDepth + 1, true, false, time);
        explanationManager.logExplanationsWithLevel(currentDepth, true,false, time);

    }

    @Override
    protected boolean canBePruned(Explanation explanation) throws OWLOntologyCreationException {
        if (!checkRules.isMinimal(explanationManager.getPossibleExplanations(), explanation)){
            return true;
        }
        if(pathsInCertainDepth.contains(path)){
            return true;
        }
        pathsInCertainDepth.add(new HashSet<>(path));

        if(Configuration.CHECK_RELEVANCE_DURING_BUILDING_TREE_IN_MHS_MXP){
            if(!checkRules.isRelevant(explanation)){
                return true;
            }
        }

        ////////////////////////////
        if (checkRules.isExplanation(explanation)){
                addToExplanations(explanation);
                return true;
            }
        ///////////////////////////
        return false;
    }

    @Override
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

    private boolean addNewExplanations(){
        List<Explanation> newExplanations = findExplanations();
        explanationManager.setLengthOneExplanations(new ArrayList<>());
        for (Explanation conflict : newExplanations){
            if (conflict.getOwlAxioms().size() == 1){
                //explanationManager.addLengthOneExplanation(conflict.getOwlAxioms().stream().findFirst().orElse(null));
                explanationManager.addLengthOneExplanation(Iterables.get(conflict.getOwlAxioms(), 0));
            }
            conflict.addAxioms(path);
            if (checkRules.isMinimal(explanationManager.getPossibleExplanations(), conflict)){
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

    private List<Explanation> findExplanations(){
        abd_literals.removeAxioms(path);
        abd_literals.removeAxioms(explanationManager.getLengthOneExplanations());
        if(Configuration.CACHED_CONFLICTS_LONGEST_CONFLICT){
            setDivider.setIndexesOfExplanations(explanationManager.getPossibleExplanationsCount());
        }
        Conflict conflict = findConflicts(abd_literals);
        abd_literals.addAxioms(path);
        abd_literals.addAxioms(explanationManager.getLengthOneExplanations());
        return conflict.getExplanations();
    }

    private Conflict findConflicts(Axioms literals) {
        path.remove(negObservation);
        reasonerManager.addAxiomsToOntology(path);

        if (isTimeout()) {
            return new Conflict(new Axioms(), new LinkedList<>());
        }

        if (isOntologyWithLiteralsConsistent(literals.getAxiomSet())) {
            return new Conflict(literals, new LinkedList<>());
        }
        resetOntologyToOriginal();
        if (literals.getAxiomSet().size() == 1) {
            List<Explanation> explanations = new LinkedList<>();
            explanations.add(new Explanation(literals.getAxiomSet(), literals.getAxiomSet().size(), currentDepth, threadTimes.getTotalUserTimeInSec()));
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
        conflictLiterals.getAxiomSet().addAll(conflictC1.getLiterals().getAxiomSet());
        conflictLiterals.getAxiomSet().addAll(conflictC2.getLiterals().getAxiomSet());

        while (!isOntologyWithLiteralsConsistent(conflictLiterals.getAxiomSet())) {

            if ((Configuration.DEPTH == null || Configuration.DEPTH == 0 || Configuration.DEPTH == Integer.MAX_VALUE) && Configuration.TIMEOUT != null)
                progressManager.updateProgress(currentDepth, threadTimes.getTotalUserTimeInSec());

            if (isTimeout()) break;

            path.addAll(conflictC2.getLiterals().getAxiomSet());
            Explanation X = getConflict(conflictC2.getLiterals().getAxiomSet(), conflictC1.getLiterals(), path);
            path.removeAll(conflictC2.getLiterals().getAxiomSet());

            path.addAll(X.getOwlAxioms());
            Explanation CS = getConflict(X.getOwlAxioms(), conflictC2.getLiterals(), path);
            path.removeAll(X.getOwlAxioms());

            CS.getOwlAxioms().addAll(X.getOwlAxioms());

            conflictLiterals.getAxiomSet().removeAll(conflictC1.getLiterals().getAxiomSet());
            X.getOwlAxioms().stream().findFirst().ifPresent(axiom -> conflictC1.getLiterals().getAxiomSet().remove(axiom));
            conflictLiterals.getAxiomSet().addAll(conflictC1.getLiterals().getAxiomSet());

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

    private Explanation getConflict(Collection<OWLAxiom> axioms, Axioms literals, Set<OWLAxiom> actualPath) {

        if (isTimeout()) {
            return new Explanation();
        }

        if (!axioms.isEmpty() && !isOntologyConsistent()) {
            return new Explanation();
        }

        if (literals.getAxiomSet().size() == 1) {
            return new Explanation(literals.getAxiomSet(), 1, currentDepth, threadTimes.getTotalUserTimeInSec());
        }

        List<Axioms> sets = setDivider.divideIntoSetsWithoutCondition(literals);

        actualPath.addAll(sets.get(0).getAxiomSet());
        Explanation D2 = getConflict(sets.get(0).getAxiomSet(), sets.get(1), actualPath);
        actualPath.removeAll(sets.get(0).getAxiomSet());

        actualPath.addAll(D2.getOwlAxioms());
        Explanation D1 = getConflict(D2.getOwlAxioms(), sets.get(0), actualPath);
        actualPath.removeAll(D2.getOwlAxioms());

        Set<OWLAxiom> conflicts = new HashSet<>();
        conflicts.addAll(D1.getOwlAxioms());
        conflicts.addAll(D2.getOwlAxioms());

        return new Explanation(conflicts, conflicts.size(), currentDepth, threadTimes.getTotalUserTimeInSec());
    }

}
