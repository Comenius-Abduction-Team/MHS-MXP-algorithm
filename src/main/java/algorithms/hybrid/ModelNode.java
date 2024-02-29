package algorithms.hybrid;

import org.semanticweb.owlapi.model.OWLAxiom;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ModelNode extends TreeNode {

    public Set<OWLAxiom> data;
    public boolean modelIsValid = true;
    private final Set<OWLAxiom> lenghtOneExplanations = new HashSet<>();
    public int min, index, parentIndex = 0;

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ModelNode) {
            ModelNode node = (ModelNode) obj;
            return data.containsAll(node.data) && node.data.containsAll(data);
        }
        return false;
    }

    public void addLengthOneExplanations(List<OWLAxiom> explanations){
        lenghtOneExplanations.addAll(explanations);
    }

    public void addLengthOneExplanationsFromNode(ModelNode node){
        lenghtOneExplanations.addAll(node.lenghtOneExplanations);
    }

    public Set<OWLAxiom> getLengthOneExplanations(){
        return lenghtOneExplanations;
    }
}
