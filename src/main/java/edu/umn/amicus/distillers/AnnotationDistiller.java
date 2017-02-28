package edu.umn.amicus.distillers;

import edu.umn.amicus.AnalysisPiece;
import edu.umn.amicus.PreAnnotation;

import java.util.List;

/**
 * Interface for classes that take Annotations from all types/systems and distill them to a single Annotation of any Type.
 *
 * Created by gpfinley on 10/20/16.
 */
public interface AnnotationDistiller<T> extends AnalysisPiece {

    PreAnnotation<T> distill(List<PreAnnotation> annotations);

}