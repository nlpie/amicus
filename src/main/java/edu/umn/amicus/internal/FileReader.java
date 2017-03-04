package edu.umn.amicus.internal;

import edu.umn.amicus.uimacomponents.Util;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * For internal use
 *
 * Created by gpfinley on 10/14/16.
 */
@Deprecated
public class FileReader extends CollectionReader_ImplBase {

    private static final Logger LOGGER = Logger.getLogger(FileReader.class.getName());

    private int index;
    private List<File> files;

    public final static String SYSTEMS_DATA_DIR = "systemsDataDirectory";

    @Override
    /**
     * Loads all filenames from the two specified input directories and finds those with common names
     */
    public void initialize() {
        LOGGER.info("Initializing file reader.");
        String dataDir = (String) getConfigParameterValue(SYSTEMS_DATA_DIR);
        files = Arrays.asList(new File(dataDir).listFiles());
    }

    /**
     * Add a view and annotation to this CAS containing paths to the XMI outputs of both systems
     * @param cas
     */
    @Override
    public void getNext(CAS cas) throws CollectionException, IOException {
        try {
            XmiCasDeserializer.deserialize(new FileInputStream(files.get(index)), cas);
        } catch (SAXException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
        String fname = files.get(index).getName();
        try {
            Util.setDocumentID(cas, fname.substring(0, fname.lastIndexOf('.')));
        } catch (CASException e) {
            throw new CollectionException(e);
        }
        index++;
    }

    @Override
    public Progress[] getProgress() {
        return new Progress[] { new ProgressImpl(index, files.size(), Progress.ENTITIES) };
    }

    @Override
    public boolean hasNext() {
        return index < files.size();
    }

    @Override
    public void close() {
    }

}
