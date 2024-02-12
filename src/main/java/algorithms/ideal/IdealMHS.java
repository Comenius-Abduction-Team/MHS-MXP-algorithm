package algorithms.ideal;

public class IdealMHS {

    private List<Explanation> solve(){

        setupObjects();
        checkInitalConditions();

        queue = new Queue();

        Node root = createRoot();
        generateChildren(root);

        level = 0;

        while (queue.isNotEmpty()){
            node = queue.poll();
            if (node.level > level){
                updateLevel(node.level);
                if (maxDepthReached())
                    break;
            }
            processNode(node);
        }

        explanationManager.filterExplanations();
        return explanationManager.getExplanations();

    }

    private void processNode(node){

        if (isExplanation(node.path)){
            explanationManager.add(node.path);
            return;
        }
        generateChildren(node);

    }

    generateChildren(node){
        for (abducible in node.label){
            Path path = expandPath(node.path,abducible);
            if (pathShouldBePruned(path))
                continue;

            Node child = new Node(path, node.level+1);
            Model model = findModel(path);
            if (badModel(model))
                continue;

            node.labelWithModel(model);
            queue.add(node);
        }
    }

    Model findModel(path){
        Model model = modelManager.findReusableModel();
        if (model == null){
            model = modelManager.findNewModel();
        }
        return model;
    }

}
