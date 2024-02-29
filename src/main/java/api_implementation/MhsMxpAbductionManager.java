package api_implementation;

import abduction_api.abducible.*;
import abduction_api.exception.*;
import abduction_api.manager.ExplanationWrapper;
import abduction_api.manager.MultiObservationManager;
import abduction_api.manager.ThreadAbductionManager;
import abduction_api.monitor.AbductionMonitor;
import algorithms.hybrid.HybridSolver;
import common.Configuration;
import file_logger.FileLogger;
import models.Explanation;
import org.semanticweb.owlapi.model.*;
import reasoner.*;
import timer.ThreadTimes;

import java.util.*;
import java.util.stream.Collectors;

public class MhsMxpAbductionManager implements MultiObservationManager, ThreadAbductionManager {

    private MhsMxpAbducibleContainer abducibles;
    private MhsMxpExplanationConfigurator configurator = new MhsMxpExplanationConfigurator();
    private final AbductionMonitor abductionMonitor = new AbductionMonitor();

    OWLOntology backgroundKnowledge;
    Set<OWLAxiom> observations = new HashSet<>();

    Set<ExplanationWrapper> explanations = new HashSet<>();
    String message = "";
    StringBuilder logs = new StringBuilder();

    double timeout = 0;
    int depth = 0;
    boolean pureMhs = false;
    boolean hst = false;
    boolean strictRelevance = true;

    boolean multithread = false;

    HybridSolver solver;
    ApiLoader loader;
    ReasonerManager reasonerManager;
    ThreadTimes timer;

    public boolean isMultithread() {
        return multithread;
    }

    public MhsMxpAbductionManager(){
        FileLogger.initializeLogger();
    }

    public MhsMxpAbductionManager(OWLOntology backgroundKnowledge, OWLAxiom observation)
    throws InvalidObservationException {
        setBackgroundKnowledge(backgroundKnowledge);
        setObservation(observation);
    }

    public MhsMxpAbductionManager(OWLOntology backgroundKnowledge, Set<OWLAxiom> observation)
    throws InvalidObservationException {
        setBackgroundKnowledge(backgroundKnowledge);
        setMultiAxiomObservation(observation);
    }

    void setExplanations(Collection<Explanation> explanations){
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
        else
            throwInvalidObservationException(axiom);
    }

    private void throwInvalidObservationException(OWLAxiom axiom){
        throw new InvalidObservationException(axiom);
    }

    private boolean checkObservationType(OWLAxiom axiom){
        return true;
//        AxiomType<?> type = axiom.getAxiomType();
//        return  AxiomType.CLASS_ASSERTION == type ||
//                AxiomType.OBJECT_PROPERTY_ASSERTION == type ||
//                AxiomType.NEGATIVE_OBJECT_PROPERTY_ASSERTION == type;
    }

    @Override
    public void setMultiAxiomObservation(Set<OWLAxiom> observation) throws InvalidObservationException {
        Set<OWLAxiom> validObservations = new HashSet<>();
        observation.forEach(axiom -> addObservationToSet(axiom, validObservations));
        this.observations = validObservations;
    }

    private void addObservationToSet(OWLAxiom axiom, Set<OWLAxiom> set) throws MultiObservationException, InvalidObservationException {
        if (checkObservationType(axiom))
            set.add(axiom);
        else throwInvalidObservationException(axiom);
    }

    @Override
    public OWLAxiom getObservation() throws MultiObservationException {
        if (observations.isEmpty())
            return null;
        if (observations.size() > 1)
            throw new MultiObservationException(
                    "There are multiple observations in this abduction manager. Use getMultiAxiomObservation().");
        else return new ArrayList<>(observations).get(0);
    }

    @Override
    public Set<OWLAxiom> getMultiAxiomObservation() {
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
        String[] arguments = s.split("\\s+");
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
                            throw new InvalidSolverParameterException(arguments[i], "Unknown solver argument");
                    }
                } catch(NumberFormatException e){
                    throw new InvalidSolverParameterException(arguments[i+1], "Invalid integer value");
                } catch(ArrayIndexOutOfBoundsException e){
                    throw new InvalidSolverParameterException(arguments[i], "Missing parameter value");
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
        abductionMonitor.clearMonitor();
    }

    private void setupSolver() {

        loader = new ApiLoader(this);
        ApiPrinter printer = new ApiPrinter(this);

        try {
            loader.initialize(ReasonerType.JFACT);
        } catch (Exception e){
            printer.logError("An error occurred while initialising the internal reasoner: ",e);
            return;
        }

        reasonerManager = new ReasonerManager(loader);

        ApiExplanationManager explanationManager = new ApiExplanationManager(loader, reasonerManager, this);
        ApiProgressManager progressManager = new ApiProgressManager(this);

        timer = new ThreadTimes(100);
        timer.start();
        long currentTimeMillis = System.currentTimeMillis();

        setSolverConfiguration();

        solver = new HybridSolver(timer, explanationManager, progressManager, printer);

    }

    private void solve(){
        try {
            solver.solve(loader, reasonerManager);
        } catch (Throwable e) {
            new ApiPrinter(this).logError("An error occured while solving: ", e);
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
        //Configuration.HST = hst;
        Configuration.STRICT_RELEVANCE = strictRelevance;

        setDepthInConfiguration();
        setTimeoutInConfiguration();

        if (configurator == null)
            return;

        setExplanationConfiguration();
    }

    private void setDepthInConfiguration() {
        if (depth == 0)
            Configuration.DEPTH = null;
        else if (depth > 0)
            Configuration.DEPTH = depth;
    }

    private void setTimeoutInConfiguration() {
        if (timeout == 0)
            Configuration.TIMEOUT = null;
        if (timeout > 0)
            Configuration.TIMEOUT = (long) timeout;
    }

    private void setExplanationConfiguration() {
        Configuration.LOOPING_ALLOWED = configurator.areLoopsAllowed();
        Configuration.ROLES_IN_EXPLANATIONS_ALLOWED = configurator.areRoleAssertionsAllowed();
        Configuration.NEGATION_ALLOWED = configurator.areConceptComplementsAllowed();
    }

    @Override
    public void setAbducibleContainer(AbducibleContainer abducibles) {

        if (abducibles instanceof MhsMxpAbducibleContainer)
            this.abducibles = (MhsMxpAbducibleContainer) abducibles;

        else if (abducibles instanceof SymbolAbducibleContainer == abducibles instanceof AxiomAbducibleContainer)
            throw new CommonException("Abducible container type not compatible with abduction manager!");

        else if (abducibles instanceof SymbolAbducibleContainer)
            this.abducibles = ApiObjectConverter.convertSymbolAbducibles(abducibles);

        else
            this.abducibles = ApiObjectConverter.convertAxiomAbducibles(abducibles);

    }

    @Override
    public MhsMxpAbducibleContainer getAbducibleContainer() {
        return abducibles;
    }

    @Override
    public ExplanationConfigurator getExplanationConfigurator() {
        return configurator;
    }

    @Override
    public void setExplanationConfigurator(ExplanationConfigurator configurator) {

        if (configurator instanceof MhsMxpExplanationConfigurator){
            this.configurator = (MhsMxpExplanationConfigurator) configurator;
            return;
        }

        else if (ApiObjectConverter.configuratorImplementsIncompatibleInterfaces(configurator))
            throw new CommonException("Explanation configurator type not compatible with abduction manager!");

        this.configurator = ApiObjectConverter.attemptConfiguratorConversion(configurator);

    }

    @Override
    public void run() {
        synchronized (abductionMonitor){
            multithread = true;
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

    public void setHst(boolean hst){this.hst = hst;}

    public boolean isStrictRelevance() {
        return strictRelevance;
    }

    public void setStrictRelevance(boolean strictRelevance) {
        this.strictRelevance = strictRelevance;
    }

    void appendToLog(String message){
        logs.append(message);
        logs.append('\n');
    }

    public void setMessage(String message){
        this.message = message;
    }
}
