package edu.umn.amicus.summary;

import edu.umn.amicus.util.AlignedTuple;
import edu.umn.amicus.AmicusException;
import edu.umn.amicus.config.ClassConfigurationLoader;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

/**
 * Adds specified fields to an Elasticsearch index.
 *
 */
public class ElasticsearchIndexingSummarizer extends Summarizer implements DocumentSummarizer, CollectionSummarizer {

    private static String[] esFieldNames = new String[]{};
    private static String esHost = "localhost";
    private static String esIndexName = "amicusdocs";
    private static String esTypeName = "amicusdoc";
    private static String esIdField = "docId";
    private static int esPort = 9300;

    public ElasticsearchIndexingSummarizer(String[] viewNames, String[] types, String[] fields) {
        super(viewNames, types, fields);
    }

    @Override
    public String getFileExtension() {
        return "log";
    }

    @Override
    public String summarizeDocument(Iterator<AlignedTuple> tuples, String docId, String docText) throws AmicusException {

        Client client;
        try {
            client = TransportClient.builder().build()
                    .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(esHost), esPort));
        } catch (UnknownHostException e) {
            throw new AmicusException(e);
        }

        BulkRequestBuilder bulkRequestBuilder = client.prepareBulk();

        IndexRequestBuilder indexRequestBuilder = client.prepareIndex()
                .setIndex(esIndexName)
                .setType(esTypeName)
                .setId(docId);

        List<List<String>> valuesLol = null;
        while (tuples.hasNext()) {
            AlignedTuple tuple = tuples.next();
            if (valuesLol == null) {
                valuesLol = initValuesListOfLists(tuple.size());
            }
            for (int i=0; i<tuple.size(); i++) {
                if (tuple.get(i) == null || tuple.get(i).getValue() == null)
                    continue;
                String value = String.valueOf(tuple.get(i).getValue());
                if (value != null && value.length() > 0 && !"null".equals(value)) {
                    valuesLol.get(i).add(value);
                }
            }
        }
        XContentBuilder xcb = buildContent(valuesLol, docId, docText);
        bulkRequestBuilder.add(indexRequestBuilder.setSource(xcb));
        bulkRequestBuilder.execute();
        client.close();

        try {
            return xcb.string();
        } catch (IOException e) {
            // todo: is this the way to return a stack trace?
            return e.toString();
        }
    }

    @Override
    public String summarizeCollection(Iterator<AlignedTuple> tuples, Iterator<String> docIds) throws AmicusException {

        Client client;
        try {
            client = TransportClient.builder().build()
                    .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(esHost), esPort));
        } catch (UnknownHostException e) {
            throw new AmicusException(e);
        }

        BulkRequestBuilder bulkRequestBuilder = client.prepareBulk();

        IndexRequestBuilder indexRequestBuilder = client.prepareIndex()
                .setIndex(esIndexName)
                .setType(esTypeName);

        // Go through all tuples and save annotations across each document
        // Whenever the document changes, save all additions to the bulk request builder and start a new doc (matrix)
        String lastDocId = null;
        List<List<String>> valuesLol = null;
        while (tuples.hasNext()) {
            AlignedTuple tuple = tuples.next();
            String docId = docIds.next();
            if (!docId.equals(lastDocId)) {
                if (lastDocId != null) {
                    bulkRequestBuilder.add(indexRequestBuilder.setSource(buildContent(valuesLol, lastDocId)));
                    bulkRequestBuilder.execute();
                    bulkRequestBuilder = client.prepareBulk();
                }
                valuesLol = initValuesListOfLists(tuple.size());
                lastDocId = docId;
            }
            for (int i=0; i<tuple.size(); i++) {
                if (tuple.get(i) == null || tuple.get(i).getValue() == null)
                    continue;
                String value = String.valueOf(tuple.get(i).getValue());
                if (value != null && value.length() > 0 && !"null".equals(value)) {
                    valuesLol.get(i).add(value);
                }
            }
        }
        bulkRequestBuilder.add(indexRequestBuilder.setSource(buildContent(valuesLol, lastDocId)));

        bulkRequestBuilder.execute();
        client.close();

        // todo: logging as return value?
        return "";
    }

    private static List<List<String>> initValuesListOfLists(int n) {
        List<List<String>> valuesLol = new ArrayList<>();
        for (int i=0; i<n; i++) {
            valuesLol.add(new ArrayList<String>());
        }
        return valuesLol;
    }

    private XContentBuilder buildContent(List<List<String>> valueMat, String docId) throws AmicusException {
        return buildContent(valueMat, docId, null);
    }

    private XContentBuilder buildContent(List<List<String>> valueMat, String docId, @Nullable String docText) throws AmicusException {
        try {
            XContentBuilder xContentBuilder = XContentFactory.jsonBuilder()
                    .startObject()
                    .field(esIdField, docId);
            if (docText != null) {
                xContentBuilder.field("documentText", docText);
            }
            // this is where you set a name to the array of values
            for (int i = 0; i < valueMat.size(); i++) {
                // Use the name of the UIMA field if the user hasn't configured enough elasticsearch field names for this class config
                String field = i >= esFieldNames.length ? fields[i] : esFieldNames[i];
                if (field == null) {
                    field = "field" + i;
                }
                xContentBuilder.startArray(field);
                for (String value : valueMat.get(i)) {
                    xContentBuilder.value(value);
                }
                xContentBuilder.endArray();
            }
            return xContentBuilder.endObject();
        } catch (IOException e) {
            throw new AmicusException(e);
        }
    }

    private static class Config {
        public String[] esFieldNames;
        public String esHost;
        public String esIndexName;
        public String esTypeName;
        public String esIdField;
        public Integer esPort;
    }

    static {
        Config config;
        try {
            config = (Config) ClassConfigurationLoader.load(Config.class);
        } catch (FileNotFoundException e) {
            config = null;
        }
        if (config != null) {
            if (config.esFieldNames != null) {
                esFieldNames = config.esFieldNames;
            }
            if (config.esHost != null) {
                esHost = config.esHost;
            }
            if (config.esIndexName != null) {
                esIndexName = config.esIndexName;
            }
            if (config.esTypeName != null) {
                esTypeName = config.esTypeName;
            }
            if (config.esIdField != null) {
                esIdField = config.esIdField;
            }
            if (config.esPort != null) {
                esPort = config.esPort;
            }
        }
    }

}
