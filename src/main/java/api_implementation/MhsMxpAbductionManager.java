package api_implementation;

import abduction_api.abducibles.AbducibleContainer;
import abduction_api.exception.InvalidObservationException;
import abduction_api.exception.InvalidSolverSettingException;
import abduction_api.exception.MultiObservationException;
import abduction_api.manager.AbductionManager;
import abduction_api.manager.ExplanationWrapper;
import abduction_api.manager.MultiObservationManager;
import abduction_api.manager.ThreadAbductionManager;
import abduction_api.monitors.AbductionMonitor;
import algorithms.hybrid.ApiExplanationManager;
import algorithms.hybrid.HybridSolver;
import common.ApiPrinter;
import common.Configuration;
import file_logger.FileLogger;
import models.Explanation;
import org.semanticweb.owlapi.model.*;
import progress.ApiProgressManager;
import reasoner.*;
import timer.ThreadTimes;

import java.util.*;
import java.util.stream.Collectors;

public class MhsMxpAbductionManager implements
        AbductionManager,
        MultiObservationManager,
        ThreadAbductionManager {

    MhsMxpAbducibleContainer abducibles;
    OWLOntology backgroundKnowledge;
    Set<OWLAxiom> observations;
    double timeout = 0;
    int depth = 0;
    boolean pureMhs = false;
    boolean strictRelevance = true;

    public boolean isMultithread() {
        return multithread;
    }

    boolean multithread = false;
    Set<ExplanationWrapper> explanations = new HashSet<>();
    final AbductionMonitor abductionMonitor = new AbductionMonitor();
    HybridSolver solver;
    ApiLoader loader;
    ReasonerManager reasonerManager;
    ThreadTimes timer;
    String message = "";
    StringBuilder logs = new StringBuilder();

    MhsMxpAbductionManager(){
        FileLogger.initializeLogger();
    }



    public void setExplanations(Collection<Explanation> explanations){
        this.explanations = explanations.stream()
                                        .map(Explanation::createExplanationWrapper)
                                        .collect(Collectors.toSet());
    }

    @Override
    public void setBackgroundKnowledge(OWLOntology ontology) {
        backgroundKnowledge = ontology;
    }

    @Override
    public OWLOntology getBackgroundKnowledge() {
        return backgroundKnowledge;
    }

    @Override
    public void setObservation(OWLAxiom axiom) throws MultiObservationException, InvalidObservationException {
        if (checkObservationType(axiom))
            observations = Collections.singleton(axiom);
        else throwInvalidObservationException(axiom);
    }

    private void throwInvalidObservationException(OWLAxiom axiom){
        throw new InvalidObservationException(axiom);
    }

    private boolean checkObservationType(OWLAxiom axiom){
        AxiomType<?> type = axiom.getAxiomType();
        return  AxiomType.CLASS_ASSERTION == type ||
                AxiomType.OBJECT_PROPERTY_ASSERTION == type ||
                AxiomType.NEGATIVE_OBJECT_PROPERTY_ASSERTION == type;
    }

    @Override
    public void setMultipleObservations(Set<OWLAxiom> observation) throws InvalidObservationException {
        observation.forEach(this::addSingleObservation);
    }

    private void addSingleObservation(OWLAxiom axiom) throws MultiObservationException, InvalidObservationException {
        if (checkObservationType(axiom))
            observations.add(axiom);
        else throwInvalidObservationException(axiom);
    }

    @Override
    public OWLAxiom getObservation() throws MultiObservationException {
        if (observations.isEmpty())
            return null;
        if (observations.size() > 1)
            throw new MultiObservationException("There are multiple observations in this abduction manager.");
        else return new ArrayList<>(observations).get(0);
    }

    @Override
    public Set<OWLAxiom> getMultipleObservations() {
        return observations;
    }

    @Override
    public void setTimeout(double seconds) {
        timeout = seconds;
    }

    @Override
    public double getTimeout() {
        return timeout;
    }

    @Override
    public void setSolverSpecificParameters(String s) {
        if (s.equals(""))
            return;
        String[] arguments = s.split(" ");
            for (int i = 0; i < arguments.length; i++){
                try {
                    switch (arguments[i]) {
                        case "-d":
                            int depth = Integer.parseInt(arguments[i + 1]);
                            setDepth(depth);
                            i++;
                            continue;
                        case "-mhs":
                            boolean pureMhs = Boolean.parseBoolean(arguments[i + 1]);
                            setPureMhs(pureMhs);
                            i++;
                            continue;
                        case "-sR":
                            boolean strictRelevance = Boolean.parseBoolean(arguments[i + 1]);
                            setStrictRelevance(strictRelevance);
                            i++;
                            continue;
                        default:
                            throw new InvalidSolverSettingException(arguments[i], "Unknown solver argument");
                    }
                } catch(NumberFormatException e){
                    throw new InvalidSolverSettingException(arguments[i+1], "Invalid integer value");
                }
            }
    }

    @Override
    public void resetSolverSpecificParameters() {
        setDepth(0);
        setPureMhs(false);
        setStrictRelevance(true);
    }

    @Override
    public void solveAbduction() {
        clearResults();
        setupSolver();
        solve();
    }

    private void clearResults() {
        explanations = new HashSet<>();
        message = "";
        logs = new StringBuilder();
    }

    private void setupSolver(){

        loader = new ApiLoader(this);

        try {
            loader.initialize(ReasonerType.JFACT);
        } catch (Exception e) {
            e.printStackTrace();
        }

        reasonerManager = new ReasonerManager(loader);

        ApiExplanationManager explanationManager = new ApiExplanationManager(loader, reasonerManager, this);
        ApiProgressManager progressManager = new ApiProgressManager(this);

        timer = new ThreadTimes(100);
        timer.start();
        long currentTimeMillis = System.currentTimeMillis();

        solver = new HybridSolver(timer, currentTimeMillis, explanationManager, progressManager,
                new ApiPrinter(this));

        setSolverConfiguration();

    }

    private void solve(){
        try {
            solver.solve(loader, reasonerManager);
        } catch (Exception e) {
            message = e.getMessage();
        }
    }

    @Override
    public Set<ExplanationWrapper> getExplanations() {
        return explanations;
    }

    @Override
    public String getOutputMessage() {
        return message;
    }

    @Override
    public String getFullLog() {
        return logs.toString();
    }

    private void setSolverConfiguration(){

        Configuration.MHS_MODE = pureMhs;
        Configuration.STRICT_RELEVANCE = strictRelevance;

        if (depth > 0) Configuration.DEPTH = depth;
        if (timeout > 0) Configuration.TIMEOUT = (long) timeout;

        if (abducibles == null)
            return;

        Configuration.LOOPING_ALLOWED = abducibles.areLoopsAllowed();
        Configuration.ROLES_IN_EXPLANATIONS_ALLOWED = abducibles.areRoleAssertionsAllowed();
        Configuration.NEGATION_ALLOWED = abducibles.areConceptComplementsAllowed();
    }


    @Override
    public void setAbducibleContainer(AbducibleContainer abducibles) {
        if (! (abducibles instanceof MhsMxpAbducibleContainer))
            return;
        this.abducibles = (MhsMxpAbducibleContainer) abducibles;
    }

    @Override
    public MhsMxpAbducibleContainer getAbducibleContainer() {
        return abducibles;
    }

    @Override
    public void run() {
        synchronized (abductionMonitor){
            multithread = true;
            abductionMonitor.clearMonitor();
            solveAbduction();
            multithread = false;
        }
    }

    @Override
    public AbductionMonitor getAbductionMonitor() {
        return abductionMonitor;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public boolean isPureMhs() {
        return pureMhs;
    }

    public void setPureMhs(boolean pureMhs) {
        this.pureMhs = pureMhs;
    }

    public boolean isStrictRelevance() {
        return strictRelevance;
    }

    public void setStrictRelevance(boolean strictRelevance) {
        this.strictRelevance = strictRelevance;
    }

    public void appendToLog(String message){
        logs.append(message);
        logs.append('\n');
    }

    public void setMessage(String message){
        this.message = message;
    }
}
