import abduction_api.abducible.SymbolAbducibleContainer;
import abduction_api.manager.ExplanationWrapper;
import abduction_api.manager.MultiObservationManager;
import abduction_api.manager.ThreadAbductionManager;
import abduction_api.monitor.AbductionMonitor;
import abduction_api.monitor.Percentage;
import algorithms.ISolver;
import algorithms.hybrid.ConsoleExplanationManager;
import algorithms.hybrid.HybridSolver;
import api_implementation.MhsMxpAbductionFactory;
import application.Application;
import application.ExitCode;
import common.ConsolePrinter;
import file_logger.FileLogger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import parser.ArgumentParser;
import common.Configuration;
import progress.ConsoleProgressManager;
import reasoner.*;
import timer.ThreadTimes;

import java.io.*;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Logger;

public class Main {

    /** whether the solver is being run from an IDE*/
    private static final boolean TESTING = false;
    /** whether the solver is being run from an IDE through the API*/
    private static final boolean API = false;

    public static void main(String[] args) throws Exception {

        FileLogger.initializeLogger();

        if (TESTING){
            if (API){
                runApiTestingMain();
                return;
            }
            args = new String[1];
            args[0] = "./in/complex_obs/pokus.in";
        }

        Logger logger = Logger.getLogger(Main.class.getSimpleName());
        ThreadTimes threadTimes = new ThreadTimes(100);

        try{

            ArgumentParser argumentParser = new ArgumentParser();
            argumentParser.parse(args);

            threadTimes.start();

            ILoader loader = new ConsoleLoader();
            loader.initialize(Configuration.REASONER);

            IReasonerManager reasonerManager = new ReasonerManager(loader);


            ISolver solver = createSolver(threadTimes, loader, reasonerManager, logger);
            solver.solve(loader, reasonerManager);

        } catch(RuntimeException e) {
            new ConsolePrinter(logger).logError("An error occurred: ", e);
            Application.finish(ExitCode.ERROR);
        } finally {
            threadTimes.interrupt();
        }

    }

    private static void runApiTestingMain() throws OWLOntologyCreationException {
        MhsMxpAbductionFactory factory = MhsMxpAbductionFactory.getFactory();

        OWLOntologyManager ontologyManager = OWLManager.createOWLOntologyManager();
        File file = new File("files/testExtractingModel9_2.owl");
        OWLOntology ont = ontologyManager.loadOntologyFromOntologyDocument(file);

        OWLDataFactory dataFactory = ontologyManager.getOWLDataFactory();
        String prefix = "http://www.co-ode.org/ontologies/ont.owl#";
        PrefixManager pm = new DefaultPrefixManager(prefix);
        OWLClass A = dataFactory.getOWLClass(":A", pm);
        OWLClass C = dataFactory.getOWLClass(":C", pm);
        OWLClass E = dataFactory.getOWLClass(":E", pm);
        OWLNamedIndividual a = dataFactory.getOWLNamedIndividual(":a", pm);
        OWLClassAssertionAxiom classAssertion = dataFactory.getOWLClassAssertionAxiom(
                dataFactory.getOWLObjectIntersectionOf(A,C,E), a);

        SymbolAbducibleContainer container = factory.getSymbolAbducibleContainer();
        container.addSymbol(A);
        container.addSymbol(C);

        MultiObservationManager m = factory.getMultiObservationAbductionManager(ont, Collections.singleton(classAssertion));
        m.setSolverSpecificParameters("");

        ThreadAbductionManager tam = (ThreadAbductionManager) m;
        AbductionMonitor monitor = tam.getAbductionMonitor();
        monitor.setWaitLimit(1000);

        Thread thread = new Thread(tam);
        thread.start();

        while(true){
            try{
                synchronized (monitor){
                    monitor.wait();

                    if (monitor.areNewExplanationsAvailable()){
                        Set<ExplanationWrapper> expl = monitor.getUnprocessedExplanations();
                        System.out.println(expl);
                        monitor.markExplanationsAsProcessed();
                        monitor.clearExplanations();
                    }

                    if (monitor.isNewProgressAvailable()){
                        Percentage progress = monitor.getProgress();
                        String message = monitor.getStatusMessage();
                        System.out.println(progress + "//" + message);
                        monitor.markProgressAsProcessed();
                    }

                    if (monitor.getProgress().getValue() >= 100){
                        thread.interrupt();
                        monitor.notify();
                        break;
                    }

                    monitor.notify();
                }
            } catch(InterruptedException e){
                e.printStackTrace();
            }

        }
        System.out.println(tam.getExplanations());

        System.out.println("-----------------------------------------");
        System.out.println(tam.getOutputMessage());
        System.out.println("-----------------------------------------");
        System.out.println(tam.getFullLog());
    }

    private static ISolver createSolver(ThreadTimes threadTimes, ILoader loader, IReasonerManager reasonerManager, Logger logger) {

        ConsoleExplanationManager explanationManager = new ConsoleExplanationManager(loader, reasonerManager);
        ConsoleProgressManager progressManager = new ConsoleProgressManager();

        long currentTimeMillis = System.currentTimeMillis();

        return new HybridSolver(threadTimes, currentTimeMillis, explanationManager, progressManager,
                new ConsolePrinter(logger));
    }
}
