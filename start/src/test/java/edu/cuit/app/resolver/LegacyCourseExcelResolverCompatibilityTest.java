package edu.cuit.app.resolver;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyCourseExcelResolverCompatibilityTest {

    @Test
    void shouldKeepDeprecatedExcelResolversUnderExcelPackage() throws ClassNotFoundException {
        assertDeprecated("edu.cuit.app.poi.course.excel.CourseExcelResolver");
        assertDeprecated("edu.cuit.app.poi.course.excel.CourseExcelResolverStrategy");
        assertDeprecated("edu.cuit.app.poi.course.excel.TheoryCourseExcelResolver");
        assertDeprecated("edu.cuit.app.poi.course.excel.ExperimentalCourseResolver");
    }

    private void assertDeprecated(String className) throws ClassNotFoundException {
        Class<?> clazz = Class.forName(className);
        assertTrue(clazz.isAnnotationPresent(Deprecated.class), className + " should be deprecated");
    }
}
