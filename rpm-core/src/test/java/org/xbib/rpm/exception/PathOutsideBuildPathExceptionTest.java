package org.xbib.rpm.exception;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 *
 */
public class PathOutsideBuildPathExceptionTest {

    @Test
    public void exception() {
        PathOutsideBuildPathException ex
                = new PathOutsideBuildPathException("scan", "build");
        assertEquals("Scan path scan outside of build directory build", ex.getMessage());
    }
}
