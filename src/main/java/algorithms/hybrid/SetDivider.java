package algorithms.hybrid;

import common.Configuration;
import models.AxiomPair;
import models.Explanation;
import models.Axioms;
import org.semanticweb.owlapi.model.OWLAxiom;

import java.util.*;

public class SetDivider {

    IExplanationManager explanationManager;
    private Map<AxiomPair, Integer> tableOfAxiomPairOccurance;
    private List<Integer> numberOfAxiomPairOccurance;
    private double median = 0;
    public Set<Integer> notUsedExplanations;
    private int lastUsedIndex;

    public SetDivider(HybridSolver hybridSolver){
        this.explanationManager = hybridSolver.getExplanationManager();
        tableOfAxiomPairOccurance = new HashMap<>();
        numberOfAxiomPairOccurance = new ArrayList<>();
        notUsedExplanations = new HashSet<>();
        lastUsedIndex = -1;
    }

    public void decreaseMedian(){
        median /= 2;
        if(median < 1){
            median = 0;
        }
    }

    public void setMedian(double median){
        this.median = median;
    }

    public double getMedian(){
        return median;
    }

    public void setIndexesOfExplanations(int sizeOfCollection){
        for(int i = 0; i < sizeOfCollection; i++){
            notUsedExplanations.add(i);
        }
    }

    public void addIndexToIndexesOfExplanations(int index){
        if(index != -1){
            notUsedExplanations.add(index);
        }
    }

    public List<Axioms> divideIntoSets(Axioms literals) {
        if(Configuration.CACHED_CONFLICTS_LONGEST_CONFLICT && explanationManager.getPossibleExplanationsCount() > 0 && lastUsedIndex != -1){
            return divideIntoSetsAccordingTheLongestConflict(literals);
        } else if (Configuration.CACHED_CONFLICTS_MEDIAN && explanationManager.getPossibleExplanationsCount() > 0){
            return divideIntoSetsAccordingTableOfLiteralsPairOccurrence(literals);
        }
        return divideIntoSetsWithoutCondition(literals);
    }

    public List<Axioms> divideIntoSetsWithoutCondition(Axioms literals){
        List<Axioms> dividedLiterals = new ArrayList<>();

        dividedLiterals.add(new Axioms());
        dividedLiterals.add(new Axioms());

        int count = 0;

        for (OWLAxiom owlAxiom : literals.getAxiomSet()) {
            dividedLiterals.get(count % 2).getAxiomSet().add(owlAxiom);
            count++;
        }
        return dividedLiterals;
    }

    private List<Axioms> divideIntoSetsAccordingTheLongestConflict(Axioms literals){
        Explanation theLongestExplanation = explanationManager.getPossibleExplanations().get(lastUsedIndex);
        Set<OWLAxiom> axiomsFromExplanation = new HashSet<>(theLongestExplanation.getOwlAxioms());

        List<Axioms> dividedLiterals = new ArrayList<>();
        dividedLiterals.add(new Axioms());
        dividedLiterals.add(new Axioms());

        int count = 0;
        for(OWLAxiom owlAxiom : axiomsFromExplanation){
            if(literals.getAxiomSet().contains(owlAxiom)){
                dividedLiterals.get(count % 2).getAxiomSet().add(owlAxiom);
                count++;
            }
        }

        for(OWLAxiom owlAxiom : literals.getAxiomSet()) {
            if(!axiomsFromExplanation.contains(owlAxiom)){
                dividedLiterals.get(count % 2).getAxiomSet().add(owlAxiom);
                count++;
            }
        }
        return dividedLiterals;
    }

    public int getIndexOfTheLongestAndNotUsedConflict(){
        int indexOfLongestExp = -1;
        int length = 0;

        for(Integer i : notUsedExplanations){
            if(explanationManager.getPossibleExplanations().get(i).getDepth() > length){
                indexOfLongestExp = i;
            }
        }

        lastUsedIndex = indexOfLongestExp;
        if(indexOfLongestExp == -1){
            return -1;
        }
        notUsedExplanations.remove(indexOfLongestExp);
        return indexOfLongestExp;
    }

    private List<Axioms> divideIntoSetsAccordingTableOfLiteralsPairOccurrence(Axioms literals){
        Set<OWLAxiom> axiomsFromLiterals = new HashSet<>(literals.getAxiomSet());
        List<Axioms> dividedLiterals = new ArrayList<>();
        dividedLiterals.add(new Axioms());
        dividedLiterals.add(new Axioms());

        for(AxiomPair key : tableOfAxiomPairOccurance.keySet()){
            if(axiomsFromLiterals.contains(key.first) && axiomsFromLiterals.contains(key.second)){
                if(tableOfAxiomPairOccurance.get(key) >= median){
                    dividedLiterals.get(0).getAxiomSet().add(key.first);
                    dividedLiterals.get(1).getAxiomSet().add(key.second);
                    axiomsFromLiterals.remove(key.first);
                    axiomsFromLiterals.remove(key.second);
                }
            }
        }

        int count = 0;
        for (OWLAxiom owlAxiom : axiomsFromLiterals) {
            dividedLiterals.get(count % 2).getAxiomSet().add(owlAxiom);
            count++;
        }

        decreaseMedian();
        return dividedLiterals;
    }

    public void addPairsOfLiteralsToTable(Explanation explanation){
        LinkedList<OWLAxiom> expAxioms;
        if (explanation.getOwlAxioms() instanceof List)
            expAxioms = (LinkedList<OWLAxiom>) explanation.getOwlAxioms();
        else
            expAxioms = new LinkedList<>(explanation.getOwlAxioms());

        for(int i = 0; i < expAxioms.size(); i++){
            for(int j = i + 1; j < expAxioms.size(); j++){
                AxiomPair axiomPair = new AxiomPair(expAxioms.get(i), expAxioms.get(j));
                Integer value = tableOfAxiomPairOccurance.getOrDefault(axiomPair, 0) + 1;
                tableOfAxiomPairOccurance.put(axiomPair, value);
                addToListOfAxiomPairOccurance(value);
            }
        }
        setMedianFromListOfAxiomPairOccurance();
    }

    public void addToListOfAxiomPairOccurance(Integer value){
        int index = 0;
        for(int i = 0; i < numberOfAxiomPairOccurance.size(); i++){
            if(numberOfAxiomPairOccurance.get(i) > value){
                break;
            }
            index++;
        }
        numberOfAxiomPairOccurance.add(index, value);
    }

    private void setMedianFromListOfAxiomPairOccurance(){
        if(numberOfAxiomPairOccurance.size() == 0){
            return;
        }
        if(numberOfAxiomPairOccurance.size() % 2 == 0){
            int index = numberOfAxiomPairOccurance.size()/2;
            median = (numberOfAxiomPairOccurance.get(index - 1) + numberOfAxiomPairOccurance.get(index)) / 2.0;
        } else {
            int index = (numberOfAxiomPairOccurance.size() - 1)/2;
            median = numberOfAxiomPairOccurance.get(index);
        }
    }

}
