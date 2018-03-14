package onethreeseven.trajsuite.core.graphics;

import com.jogamp.common.nio.Buffers;
import gov.nasa.worldwind.render.DrawContext;
import onethreeseven.trajsuitePlugin.graphics.PackedVertexData;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import java.nio.Buffer;
import java.nio.DoubleBuffer;
import java.util.logging.Logger;

/**
 * The graphics util for performing common opengl tasks such as creating VBOs.
 * @author Luke Bermingham
 */
final class GraphicsUtil {


    private GraphicsUtil() {
    }

    private static boolean vbosOkay(GL2 gl) {
        if (!gl.isFunctionAvailable("glGenBuffers")
                || !gl.isFunctionAvailable("glBindBuffer")
                || !gl.isFunctionAvailable("glBufferData")
                || !gl.isFunctionAvailable("glDeleteBuffers")) {
            Logger.getLogger(GraphicsUtil.class.getName()).severe("VBOs not supported on this gpu.");
            //if no VBO no point continuing
            return false;
        }
        return true;
    }

    /**
     * Create a vertex array buffer on the GPU, bind to it, then fill that buffer with data.
     * @param gl
     * @param vertexData the vertex data all packed, i.e vvv-ccc-vvv-ccc etc. Note that the
     *                   buffer must have its limit set so that limit() * 8 == number of bytes required.
     * @return a id of the bound buffer
     */
    public static Integer createBuffer(GL2 gl, DoubleBuffer vertexData) {
        if (vbosOkay(gl)) {
            //turn on vbos bit
            gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
            //bind ourselves a new buffer
            int[] bufferNames = new int[1];
            //createAnnotation an id for buffer
            gl.glGenBuffers(1, bufferNames, 0);
            //extract the created id
            int bufferId = bufferNames[0];

            //bind the gpu's current array buffer to the given id
            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferId);
            //using the given id createAnnotation a an empty vertex buffer data store in the gpu, static draw is a hint
            Buffer buf = vertexData.rewind();
            gl.glBufferData(GL.GL_ARRAY_BUFFER,
                    (buf.limit() * Buffers.SIZEOF_DOUBLE),
                    buf,
                    GL2.GL_STATIC_DRAW);
            //unbind but not delete
            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
            //turn off vbo bit
            gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
            //push it into buffers map
            return bufferId;
        }
        return null;
    }

    public static void deleteBuffer(GL2 gl, int bufferId) {
        try {
            gl.glDeleteBuffers(1, new int[]{bufferId}, 0);
            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
            gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
        } catch (Exception e) {
            Logger.getLogger(GraphicsUtil.class.getName())
                    .severe("Could not dispose vbo: " + e.getMessage());
        }
    }

    public static void setupVertexAttributes(DrawContext drawContext, PackedVertexData packedVertexData) {

        GL2 gl = drawContext.getGL().getGL2();

        int offset = 0;
        int stride = packedVertexData.getTotalValuesPerVert();

        for (PackedVertexData.Types vertexDataType : packedVertexData.getVertexDataTypes()) {

            switch (vertexDataType) {
                case VERTEX:
                    gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
                    //three coordinates per vert, they are doubles, and they are packed tightly
                    gl.glVertexPointer(
                            vertexDataType.nValues,
                            GL2.GL_DOUBLE,
                            stride * Buffers.SIZEOF_DOUBLE,
                            offset * Buffers.SIZEOF_DOUBLE);
                    break;
                case RGB:
                case RGBA:
                    if (!drawContext.isPickingMode()) {
                        gl.glEnable(GL.GL_BLEND);      //turn on blending
                        gl.glEnable(GL2.GL_COLOR_MATERIAL);
                        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
                        //if not picking mode turn on colors and point to where the colors are
                        gl.glEnableClientState(GL2.GL_COLOR_ARRAY);
                        //set color pointer
                        gl.glColorPointer(
                                vertexDataType.nValues,
                                GL2.GL_DOUBLE,
                                stride * Buffers.SIZEOF_DOUBLE,
                                offset * Buffers.SIZEOF_DOUBLE);
                    }
                    break;
                case NORMAL:
                    gl.glEnableClientState(GL2.GL_NORMAL_ARRAY);
                    gl.glNormalPointer(
                            GL2.GL_DOUBLE,
                            stride * Buffers.SIZEOF_DOUBLE,
                            offset * Buffers.SIZEOF_DOUBLE);
                    break;
                default:
                    throw new UnsupportedOperationException(vertexDataType + " is not supported currently");
            }
            offset += vertexDataType.nValues;
        }

    }

    public static void disableVertexAttributes(DrawContext drawContext, PackedVertexData packedVertexData) {
        GL2 gl = drawContext.getGL().getGL2();

        for (PackedVertexData.Types vertexDataType : packedVertexData.getVertexDataTypes()) {
            switch (vertexDataType) {
                case VERTEX:
                    gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
                    break;
                case RGB:
                case RGBA:
                    if (!drawContext.isPickingMode()) {
                        gl.glDisableClientState(GL2.GL_COLOR_ARRAY);
                        //disable blending
                        gl.glDisable(GL.GL_BLEND);
                        gl.glDisable(GL2.GL_COLOR_MATERIAL);
                    }
                    break;
                case NORMAL:
                    gl.glDisableClientState(GL2.GL_NORMAL_ARRAY);
                    break;
                default:
                    throw new UnsupportedOperationException(vertexDataType + " is not supported currently");
            }
        }
    }

}
