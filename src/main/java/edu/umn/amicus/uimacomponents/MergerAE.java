package edu.umn.amicus.uimacomponents;

import edu.umn.amicus.*;
import edu.umn.amicus.aligners.Aligner;
import edu.umn.amicus.pushers.Pusher;
import edu.umn.amicus.distillers.Distiller;
import edu.umn.amicus.pullers.Puller;
import edu.umn.amicus.util.ANA;
import edu.umn.amicus.util.AlignedTuple;
import edu.umn.amicus.util.AnalysisPieceFactory;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

import java.util.*;
import java.util.logging.Logger;

/**
 * Created by gpfinley on 1/18/17.
 */
public class MergerAE extends JCasAnnotator_ImplBase {

    private static final Logger LOGGER = Logger.getLogger(MergerAE.class.getName());

    public static final String MY_NAME = "name";

    public static final String READ_VIEWS = "readViews";
    public static final String INPUT_TYPES = "typeClasses";
    public static final String INPUT_FIELDS = "inputFields";
    public static final String PULLER_CLASSES = "pullerClasses";

    public static final String ALIGNER_CLASS = "aligner";

    public static final String DISTILLER_CLASSES = "distillerClasses";
    public static final String PUSHER_CLASSES = "pusherClasses";
    public static final String WRITE_VIEWS = "outputViewNames";
    public static final String OUTPUT_TYPES = "outputAnnotationTypes";
    public static final String OUTPUT_FIELDS = "outputAnnotationFields";

    // Some things are not mandatory because there are defaults available (anything with CLASS).
    // Others are not mandatory because specialized pullers/pushers might not use them (but the defaults do!).

    @ConfigurationParameter(name = MY_NAME, defaultValue = "Unnamed Merger")
    private String myName;

    @ConfigurationParameter(name = READ_VIEWS)
    private String[] readViews;
    @ConfigurationParameter(name = INPUT_TYPES, mandatory = false)
    private String[] typeClassNames;
    @ConfigurationParameter(name = INPUT_FIELDS, mandatory = false)
    private String[] inputFields;
    @ConfigurationParameter(name = PULLER_CLASSES, mandatory = false)
    private String[] pullerClassNames;

    @ConfigurationParameter(name = ALIGNER_CLASS, mandatory = false)
    private String alignerClassName;

    @ConfigurationParameter(name = DISTILLER_CLASSES, mandatory = false)
    private String[] distillerClassNames;
    @ConfigurationParameter(name = PUSHER_CLASSES, mandatory = false)
    private String[] pusherClassNames;
    @ConfigurationParameter(name = WRITE_VIEWS)
    private String[] outputViewNames;
    @ConfigurationParameter(name = OUTPUT_TYPES, mandatory = false)
    private String[] outputAnnotationTypes;
    // fields should be separated by semicolons if there are more than one (and a multi-setter Creator should be used)
    @ConfigurationParameter(name = OUTPUT_FIELDS, mandatory = false)
    private String[] outputAnnotationFields;

    private List<Class> typeClasses;
    private List<Distiller<Object>> distillers;
    private List<Pusher> pushers;
    private List<Puller> pullers;
    private Aligner aligner;

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);
        LOGGER.info("Initializing Merger analysis engine.");

        int numInputs = readViews.length;
        int numOutputs = outputViewNames.length;

        // check lengths of input and output lists to hopefully detect user-caused misalignment in config file
        // These won't be in effect when running AmicusPipeline, but might catch something if just using uimaFIT.
        try {
            assert typeClassNames == null || numInputs == typeClassNames.length;
            assert pullerClassNames == null || numInputs == pullerClassNames.length;
            assert inputFields == null || numInputs == inputFields.length;
        } catch (AssertionError e) {
            LOGGER.severe(String.format("Configuration parameters for inputs do not line up! Check parameter lists for Merger \"%s\"", myName));
            throw new AmicusConfigurationException(e);
        }
        try {
            assert distillerClassNames == null || numOutputs == distillerClassNames.length;
            assert pusherClassNames == null || numOutputs == pusherClassNames.length;
            assert outputAnnotationTypes == null || numOutputs == outputAnnotationTypes.length;
            assert outputAnnotationFields == null || numOutputs == outputAnnotationFields.length;
        } catch (AssertionError e) {
            LOGGER.severe(String.format("Configuration parameters for outputs do not line up! Check parameter lists for Merger \"%s\"", myName));
            throw new AmicusConfigurationException(e);
        }

        if (pullerClassNames == null) {
            pullerClassNames = new String[numInputs];
        }
        if (typeClassNames == null) {
            typeClassNames = new String[numInputs];
        }
        if (inputFields == null) {
            inputFields = new String[numInputs];
        }
        if (distillerClassNames == null) {
            distillerClassNames = new String[numOutputs];
        }
        if (pusherClassNames == null) {
            pusherClassNames = new String[numOutputs];
        }
        if (outputAnnotationFields == null) {
            outputAnnotationFields = new String[numOutputs];
        }
        if (outputAnnotationTypes == null) {
            outputAnnotationTypes = new String[numOutputs];
        }
        distillers = new ArrayList<>();
        pushers = new ArrayList<>();
        pullers = new ArrayList<>();
        try {
            aligner = AnalysisPieceFactory.aligner(alignerClassName);
            for (String distillerClassName : distillerClassNames) {
                distillers.add(AnalysisPieceFactory.distiller(distillerClassName));
            }
            for (int i = 0; i < numOutputs; i++) {
                pushers.add(AnalysisPieceFactory.pusher(pusherClassNames[i], outputAnnotationTypes[i], outputAnnotationFields[i]));
            }
            for (int i = 0; i < numInputs; i++) {
                pullers.add(AnalysisPieceFactory.puller(pullerClassNames[i], inputFields[i]));
            }
        } catch (AmicusException e) {
            LOGGER.severe(String.format("Couldn't load analysis pieces for Merger \"%s\"", myName));
            throw new ResourceInitializationException(e);

        }

        if (typeClassNames == null) {
            typeClassNames = new String[numInputs];
        }
        typeClasses = new ArrayList<>();
        for (int i = 0; i < numInputs; i++) {
            try {
                typeClasses.add(Util.getTypeClass(typeClassNames[i]));
            } catch (AmicusException e) {
                LOGGER.severe(String.format("Could not find input type \"%s\" for Merger \"%s\". Confirm that types are" +
                        "correct and that classes have been generated from a UIMA type system (easiest way is to build" +
                        "via maven).", typeClassNames[i], myName));
                throw new ResourceInitializationException(e);
            }
        }
    }

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {

        try {
            Util.createOutputViews(jCas, outputViewNames);
        } catch (CASException e) {
            LOGGER.severe(String.format("Could not create output views for Merger \"%s\"", myName));
            throw new AnalysisEngineProcessException(e);
        }

        try {
            Iterator<AlignedTuple> listIter = aligner.alignAndIterate(getPreAnnotations(jCas));
            while (listIter.hasNext()) {
                AlignedTuple preannotations = listIter.next();
                for (int i = 0; i < outputViewNames.length; i++) {
                    JCas outputView;
                    try {
                        outputView = jCas.getView(outputViewNames[i]);
                    } catch (CASException e) {
                        throw new AnalysisEngineProcessException(e);
                    }
                    ANA<Object> distilled = distillers.get(i).distill(preannotations);
                    if (distilled != null) {
                        pushers.get(i).push(outputView, distilled);
                    }
                }
            }
        } catch (AmicusException e) {
            LOGGER.severe(String.format("Processing problem for Merger \"%s\"", myName));
            throw new AnalysisEngineProcessException(e);
        }
    }



    private List<List<ANA>> getPreAnnotations(JCas jCas) throws AnalysisEngineProcessException {
        List<List<ANA>> allAnnotations = new ArrayList<>();
        for (int i=0; i< readViews.length; i++) {
            JCas readView;
            try {
                readView = jCas.getView(readViews[i]);
            } catch (CASException e) {
                LOGGER.severe(String.format("Couldn't access view \"%s\" in Exporter \"%s\"", readViews[i], myName));
                throw new AnalysisEngineProcessException(e);
            }
            List<ANA> theseAnnotations = new ArrayList<>();
            allAnnotations.add(theseAnnotations);
            // Get all annotations of this class and add them to the index
            for (Annotation a : (FSIndex<Annotation>) readView.getAnnotationIndex(typeClasses.get(i))) {
                Object pulled;
                try {
                    pulled = pullers.get(i).pull(a);
                } catch (AmicusException e) {
                    // todo log
                    throw new AnalysisEngineProcessException(e);
                }
                if (pulled != null) {
                    theseAnnotations.add(new ANA<>(pulled, a));
                }
            }
        }

        return allAnnotations;
    }

}
