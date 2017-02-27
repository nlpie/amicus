package edu.umn.amicus;

import edu.umn.amicus.config.*;
import edu.umn.amicus.uimacomponents.*;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Standard pipeline, as configurable by yml file.
 * todo: more doc
 *
 * Created by gpfinley on 1/20/17.
 */
public class AmicusPipeline {

    public AmicusPipeline(String configFilePath) throws IOException, UIMAException {

        AmicusPipelineConfiguration pipelineConfig;

        Yaml yaml = new Yaml();
        pipelineConfig = (AmicusPipelineConfiguration) yaml.load(new FileInputStream(configFilePath));

        CollectionReader reader;
        List<AnalysisEngine> engines = new ArrayList<>();
        reader = CollectionReaderFactory.createReader(CommonFilenameCR.class,
                CommonFilenameCR.SYSTEM_DATA_DIRS, pipelineConfig.aggregateInputDirectories());

        for (SourceSystemConfig systemConfig : pipelineConfig.allSystemsUsed) {
            engines.add(AnalysisEngineFactory.createEngine(CasAdderAE.class,
                    CasAdderAE.DATA_DIR, systemConfig.dataPath,
                    CasAdderAE.READ_FROM_VIEW, systemConfig.readFromView,
                    CasAdderAE.COPY_INTO_VIEW, systemConfig.saveIntoView
                    ));
        }

        for (PipelineComponentConfig componentConfig : pipelineConfig.pipelineComponents) {
            if (componentConfig.getClass().equals(MergerConfig.class)) {
                MergerConfig mergerConfig = (MergerConfig) componentConfig;
                engines.add(
                        AnalysisEngineFactory.createEngine(MergerAE.class,
                                MergerAE.READ_VIEWS, PipelineComponentConfig.aggregateInputSystemNames(mergerConfig.inputs),
                                MergerAE.INPUT_TYPES, PipelineComponentConfig.aggregateInputTypes(mergerConfig.inputs),
                                MergerAE.INPUT_FIELDS, PipelineComponentConfig.aggregateInputFields(mergerConfig.inputs),
                                MergerAE.PULLER_CLASSES, PipelineComponentConfig.aggregateInputPullers(mergerConfig.inputs),
                                MergerAE.ALIGNER_CLASS, mergerConfig.alignerClass,
                                MergerAE.DISTILLER_CLASSES, PipelineComponentConfig.aggregateOutputDistillers(mergerConfig.outputs),
                                MergerAE.OUTPUT_TYPES, PipelineComponentConfig.aggregateOutputAnnotationClasses(mergerConfig.outputs),
                                MergerAE.OUTPUT_FIELDS, PipelineComponentConfig.aggregateOutputAnnotationFields(mergerConfig.outputs),
                                MergerAE.PUSHER_CLASSES, PipelineComponentConfig.aggregateOutputPushers(mergerConfig.outputs),
                                MergerAE.WRITE_VIEWS, PipelineComponentConfig.aggregateOutputViewNames(mergerConfig.outputs)
                        ));
            } else if(componentConfig.getClass().equals(SummarizerConfig.class)) {
                SummarizerConfig summarizerConfig = (SummarizerConfig) componentConfig;
                engines.add(
                        AnalysisEngineFactory.createEngine(SummarizerAE.class,
                                SummarizerAE.INPUT_TYPE, summarizerConfig.input.annotationType,
                                SummarizerAE.INPUT_FIELD, summarizerConfig.input.annotationField,
                                SummarizerAE.READ_VIEW, summarizerConfig.input.fromView,
                                SummarizerAE.PULLER_CLASS, summarizerConfig.input.pullerClass,
                                SummarizerAE.SUMMARY_WRITER_CLASS, summarizerConfig.summaryWriter,
                                SummarizerAE.LISTENER_NAME, summarizerConfig.name,
                                SummarizerAE.OUTPUT_PATH, summarizerConfig.outPath
                        ));
            } else if(componentConfig.getClass().equals(ExporterConfig.class)) {
                ExporterConfig exporterConfig = (ExporterConfig) componentConfig;
                engines.add(
                        AnalysisEngineFactory.createEngine(ExporterAE.class,
                                ExporterAE.READ_VIEWS, PipelineComponentConfig.aggregateInputSystemNames(exporterConfig.inputs),
                                ExporterAE.INPUT_TYPES, PipelineComponentConfig.aggregateInputTypes(exporterConfig.inputs),
                                ExporterAE.INPUT_FIELDS, PipelineComponentConfig.aggregateInputFields(exporterConfig.inputs),
                                ExporterAE.PULLER_CLASSES, PipelineComponentConfig.aggregateInputPullers(exporterConfig.inputs),
                                ExporterAE.ALIGNER_CLASS, exporterConfig.alignerClass,
                                ExporterAE.EXPORT_WRITER_CLASS, exporterConfig.exporterClass,
                                ExporterAE.OUTPUT_DIRECTORY, exporterConfig.outputDirectory
                        ));
            } else if(componentConfig.getClass().equals(TranslatorConfig.class)) {
                TranslatorConfig translatorConfig = (TranslatorConfig) componentConfig;
                engines.add(
                        AnalysisEngineFactory.createEngine(TranslatorAE.class,
                                TranslatorAE.READ_VIEW, translatorConfig.input.fromView,
                                TranslatorAE.PULLER_CLASS, translatorConfig.input.pullerClass,
                                TranslatorAE.INPUT_TYPE, translatorConfig.input.annotationType,
                                TranslatorAE.INPUT_FIELD, translatorConfig.input.annotationField,
                                TranslatorAE.FILTER_CLASS, translatorConfig.filterClassName,
                                TranslatorAE.FILTER_PATTERN, translatorConfig.filterPattern,
                                TranslatorAE.MAPPER_CONFIG_PATHS, translatorConfig.mapperConfigPaths,
                                // todo: warn if any distillers are included?
//                                TranslatorAE.DISTILLER_CLASSES, PipelineComponentConfig.aggregateOutputDistillers(translatorConfig.outputs),
                                TranslatorAE.OUTPUT_TYPES, PipelineComponentConfig.aggregateOutputAnnotationClasses(translatorConfig.outputs),
                                TranslatorAE.OUTPUT_FIELDS, PipelineComponentConfig.aggregateOutputAnnotationFields(translatorConfig.outputs),
                                TranslatorAE.PUSHER_CLASSES, PipelineComponentConfig.aggregateOutputPushers(translatorConfig.outputs),
                                TranslatorAE.WRITE_VIEWS, PipelineComponentConfig.aggregateOutputViewNames(translatorConfig.outputs)
                        ));
            } else {
                throw new AmicusException(componentConfig.getClass().getName() + " hasn't been implemented yet!");
            }
        }

        engines.add(AnalysisEngineFactory.createEngine(XmiWriterAE.class,
                XmiWriterAE.CONFIG_OUTPUT_DIR, pipelineConfig.xmiOutPath));

        SimplePipeline.runPipeline(reader, engines.toArray(new AnalysisEngine[engines.size()]));

    }

    public static void main(String[] args) throws Exception {
        // todo: remove, and show usage msg if no argument
//        args = new String[]{"simple_test_config.yml"};
//        args = new String[]{"example_export_pipeline_config.yml"};
        String configFilePath = args[0];
        new AmicusPipeline(configFilePath);
    }

}