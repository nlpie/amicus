package edu.umn.amicus.uimacomponents;

import edu.umn.amicus.AmicusException;
import edu.umn.amicus.PreAnnotation;
import edu.umn.amicus.aligners.AnnotationAligner;
import edu.umn.amicus.aligners.EachSoloAligner;
import edu.umn.amicus.exporters.AnnotationExporter;
import edu.umn.amicus.pullers.AnnotationPuller;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

/**
 * todo: doc, test
 * Created by greg on 2/11/17.
 */
public class ExporterAE extends JCasAnnotator_ImplBase {

    private static final Logger LOGGER = Logger.getLogger(ExporterAE.class.getName());

    private static final String EXPORTER_DEFAULT_ALIGNER = EachSoloAligner.class.getName();

    public static final String READ_VIEWS = "readViews";
    public static final String INPUT_TYPES = "typeClasses";
    public static final String INPUT_FIELDS = "fieldNames";
    public static final String PULLER_CLASSES = "pullerClasses";

    public static final String ALIGNER_CLASS = "alignerClass";
    public static final String EXPORTER_CLASS = "exporterClassName";
    public static final String OUTPUT_DIRECTORY = "outputDirectory";

    @ConfigurationParameter(name = READ_VIEWS)
    private String[] readViews;
    @ConfigurationParameter(name = INPUT_TYPES)
    private String[] typeClassNames;
    @ConfigurationParameter(name = INPUT_FIELDS, mandatory = false)
    private String[] fieldNames;
    @ConfigurationParameter(name = PULLER_CLASSES, mandatory = false)
    private String[] pullerClassNames;

    @ConfigurationParameter(name = ALIGNER_CLASS, mandatory = false)
    private String alignerClassName;
    @ConfigurationParameter(name = EXPORTER_CLASS, mandatory = false)
    private String exporterClassName;
    @ConfigurationParameter(name = OUTPUT_DIRECTORY)
    private String outputDirectory;

    private List<AnnotationPuller> pullers;
    private List<Class<? extends Annotation>> typeClasses;

    private AnnotationAligner aligner;
    private AnnotationExporter exporter;

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);
        LOGGER.info("Initializing Exporter analysis engine.");

        try {
            Files.createDirectories(Paths.get(outputDirectory));
        } catch (IOException e) {
            throw new ResourceInitializationException(e);
        }

        int numInputs = readViews.length;

        // check lengths of input and output lists to hopefully detect user-caused misalignment in config file
        // These won't be in effect when running AmicusPipeline, but might catch something if just using uimaFIT.
        try {
            assert numInputs == typeClassNames.length;
            assert pullerClassNames == null || numInputs == pullerClassNames.length;
            assert fieldNames == null || numInputs == fieldNames.length;
        } catch (AssertionError e) {
            throw new AmicusException("Configuration parameters for inputs do not line up! Check parameter lists.");
        }


        try {
            typeClasses = new ArrayList<>();
            for (int i = 0; i < numInputs; i++) {
                typeClasses.add((Class<? extends Annotation>) Class.forName(typeClassNames[i]));
            }
        } catch (ClassNotFoundException e) {
            LOGGER.severe("Could not add input types. Confirm that types are correct and that classes have been " +
                    "generated from a UIMA type system (easiest way is to build via maven).");
            throw new AmicusException(e);
        }

        aligner = AnnotationAligner.create(alignerClassName == null ? EXPORTER_DEFAULT_ALIGNER : alignerClassName);
        for (int i=0; i<numInputs; i++) {
            pullers.add(AnnotationPuller.create(pullerClassNames[i], fieldNames[i]));
        }
        exporter = AnnotationExporter.create(exporterClassName);

        exporter.setViewNames(readViews);
    }

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {

        // Set up a shell iterator that will call Pullers and pass along transformed values to the exporter
        final Iterator<List<Annotation>> annotationsIterator = aligner.alignAndIterate(getAnnotations(jCas));
        String text = exporter.exportContents(new Iterator<List<PreAnnotation>>() {
            @Override
            public boolean hasNext() {
                return annotationsIterator.hasNext();
            }
            @Override
            public List<PreAnnotation> next() {
                List<Annotation> annotations = annotationsIterator.next();
                List<PreAnnotation> preannotations = new ArrayList<>();
                for (int i = 0; i < annotations.size(); i++) {
                    preannotations.add(
                            new PreAnnotation<>(pullers.get(i).transform(annotations.get(i)), annotations.get(i)));
                }
                return preannotations;
            }
        });

        String docId = Util.getDocumentID(jCas.getCas());
        Path filepath = Paths.get(outputDirectory).resolve(docId + "." + exporter.getFileExtension());
        Writer writer;
        try {
            writer = new FileWriter(filepath.toFile());
            writer.write(text);
            writer.close();
        } catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        }
    }

    private List<List<Annotation>> getAnnotations(JCas jCas) {
        List<List<Annotation>> allAnnotations = new ArrayList<>();
        for (int i=0; i< readViews.length; i++) {
            JCas readView;
            try {
                readView = jCas.getView(readViews[i]);
            } catch (CASException e) {
                e.printStackTrace();
                throw new AmicusException("Couldn't view %s", readViews[i]);
            }
            List<Annotation> theseAnnotations = new ArrayList<>();
            allAnnotations.add(theseAnnotations);
            // Get all annotations of this class and add them to the index
            for (Annotation a : readView.getAnnotationIndex(typeClasses.get(i))) {
                theseAnnotations.add(a);
            }
        }

        return allAnnotations;
    }



}
