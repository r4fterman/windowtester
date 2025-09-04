package com.windowtester.junit5.resolver;

import static javax.swing.WindowConstants.EXIT_ON_CLOSE;

import com.windowtester.junit5.UIUnderTest;
import java.awt.Dimension;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Optional;
import javax.swing.JFrame;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.support.AnnotationSupport;

public class AnnotationResolver {

  private final ExtensionContext context;

  public AnnotationResolver(ExtensionContext context) {
    this.context = context;
  }

  public Optional<FieldInfo> tryToFindAnnotatedField(Class<? extends Annotation> annotationType) {
    var annotatedFieldList =
        context
            .getTestInstance()
            .map(
                testInstance ->
                    AnnotationSupport.findAnnotatedFields(testInstance.getClass(), annotationType)
                        .stream()
                        .map(field -> fieldToFieldInfo(testInstance, field, annotationType))
                        .toList())
            .orElse(Collections.emptyList());

    if (annotatedFieldList.size() > 1) {
      throw new JUnitException("Only one instance must be annotated with @UIUnderTest.");
    }
    if (annotatedFieldList.isEmpty()) {
      return Optional.empty();
    }

    var fieldInfo = annotatedFieldList.get(0);
    return Optional.ofNullable(fieldInfo);
  }

  private FieldInfo fieldToFieldInfo(
      Object testInstance, Field field, Class<? extends Annotation> annotationType) {
    try {
      var title = getTitle(field, annotationType);
      var dimension = getDimension(field, annotationType);
      field.setAccessible(true);
      var value = field.get(testInstance);
      if (value == null) {
        // field not yet instantiated
        var frame = new JFrame(title);
        frame.setDefaultCloseOperation(EXIT_ON_CLOSE);
        return new FieldInfo(title, dimension, frame);
      }
      return new FieldInfo(title, dimension, value);
    } catch (IllegalAccessException e) {
      var message =
          String.format(
              "Unable to access annotation field [%s]. Make sure it has public accessibility.",
              field.getName());
      throw new JUnitException(message, e);
    }
  }

  private Dimension getDimension(Field field, Class<? extends Annotation> annotationType) {
    Annotation annotation = field.getAnnotation(annotationType);
    if (annotation instanceof UIUnderTest uiUnderTest) {
      return new Dimension(uiUnderTest.width(), uiUnderTest.height());
    }
    return new Dimension(400, 300);
  }

  private String getTitle(Field field, Class<? extends Annotation> annotationType) {
    Annotation annotation = field.getAnnotation(annotationType);
    if (annotation instanceof UIUnderTest uiUnderTest) {
      return uiUnderTest.title();
    }
    return "";
  }
}
