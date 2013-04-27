/*
 * Copyright (c) 2005 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2010 JogAmp Community. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 */

package com.jogamp.opengl.util.texture.awt;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferDouble;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureData.PixelBufferProvider;

public class AWTTextureData extends TextureData {
    public static final PixelAttributes awtPixelAttributesIntRGB = new PixelAttributes(GL.GL_BGRA, GL.GL_UNSIGNED_BYTE);
    
    /** 
     * AWT {@link PixelBufferProvider} backed by a {@link BufferedImage} of type 
     * {@link BufferedImage#TYPE_INT_ARGB} or {@link BufferedImage#TYPE_INT_RGB}
     * and {@link #allocate(int, int, int) allocating} am array backed  {@link IntBuffer}.
     */
    public static final class AWTPixelBufferProviderInt implements PixelBufferProvider {
        private BufferedImage image = null;
        private int componentCount = 0;
        
        @Override
        public PixelAttributes getAttributes(GL gl, int componentCount) {
            this.componentCount = componentCount;
            return awtPixelAttributesIntRGB;
        }
        @Override
        public boolean requiresNewBuffer(int width, int height) {
            return null == image || image.getWidth() != width || image.getHeight() != height;
        }
        /**
         * {@inheritDoc}
         * <p>
         * Returns an array backed {@link IntBuffer} of size <pre>width*height*{@link Buffers#SIZEOF_INT SIZEOF_INT}</code>.
         * </p>
         */
        @Override
        public Buffer allocate(int width, int height, int minByteSize) {
            image = new BufferedImage(width, height, 4 == componentCount ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
            final int[] readBackIntBuffer = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
            return IntBuffer.wrap( readBackIntBuffer );
        }
        
        /** Returns the number source components being used as indicated at {@link #allocate(int, int, int)}. */
        public int getComponentCount() { return componentCount; }
        
        /** Returns the underlying {@link BufferedImage} as allocated via {@link #allocate(int, int, int)}. */
        public BufferedImage getImage() { return image; }
    }
    
    // Mechanism for lazily converting input BufferedImages with custom
    // ColorModels to standard ones for uploading to OpenGL, as well as
    // backing off from the optimizations of hoping that either
    // GL_EXT_abgr or OpenGL 1.2 are present
    private BufferedImage imageForLazyCustomConversion;
    private boolean expectingEXTABGR;
    private boolean expectingGL12;

    private static final java.awt.image.ColorModel rgbaColorModel =
        new ComponentColorModel(java.awt.color.ColorSpace.getInstance(java.awt.color.ColorSpace.CS_sRGB),
                                new int[] {8, 8, 8, 8}, true, true, 
                                Transparency.TRANSLUCENT,
                                DataBuffer.TYPE_BYTE);
    private static final java.awt.image.ColorModel rgbColorModel =
        new ComponentColorModel(java.awt.color.ColorSpace.getInstance(java.awt.color.ColorSpace.CS_sRGB),
                                new int[] {8, 8, 8, 0}, false, false,
                                Transparency.OPAQUE,
                                DataBuffer.TYPE_BYTE);


    /** 
     * Constructs a new TextureData object with the specified parameters
     * and data contained in the given BufferedImage. The resulting
     * TextureData "wraps" the contents of the BufferedImage, so if a
     * modification is made to the BufferedImage between the time the
     * TextureData is constructed and when a Texture is made from the
     * TextureData, that modification will be visible in the resulting
     * Texture.
     *
     * @param glp      the OpenGL Profile this texture data should be
     *                       created for.
     * @param internalFormat the OpenGL internal format for the
     *                       resulting texture; may be 0, in which case
     *                       it is inferred from the image's type
     * @param pixelFormat    the OpenGL internal format for the
     *                       resulting texture; may be 0, in which case
     *                       it is inferred from the image's type (note:
     *                       this argument is currently always ignored)
     * @param mipmap         indicates whether mipmaps should be
     *                       autogenerated (using GLU) for the resulting
     *                       texture
     * @param image          the image containing the texture data
     */
    public AWTTextureData(GLProfile glp, 
                          int internalFormat,
                          int pixelFormat,
                          boolean mipmap,
                          BufferedImage image) {
        super(glp);
        if (internalFormat == 0) {
            this.internalFormat = image.getColorModel().hasAlpha() ? GL.GL_RGBA : GL.GL_RGB;
        } else {
            this.internalFormat = internalFormat;
        }
        createFromImage(glp, image);
        this.mipmap = mipmap;
        if (buffer != null) {
            estimatedMemorySize = estimatedMemorySize(buffer);
        } else {
            // In the lazy custom conversion case we don't yet have a buffer
            if (imageForLazyCustomConversion != null) {
                estimatedMemorySize = estimatedMemorySize(wrapImageDataBuffer(imageForLazyCustomConversion));
            }
        }
    }

    private void validatePixelAttributes() {
        if (imageForLazyCustomConversion != null) {
            if (!((expectingEXTABGR && haveEXTABGR) ||
                  (expectingGL12    && haveGL12))) {
                revertPixelAttributes();
            }
        }        
    }
    
    @Override
    public PixelAttributes getPixelAttributes() {
        validatePixelAttributes();
        return super.getPixelAttributes();
    }
    
    @Override
    public int getPixelFormat() {
        validatePixelAttributes();
        return super.getPixelFormat();
    }
    @Override
    public int getPixelType() {
        validatePixelAttributes();
        return super.getPixelType();
    }

    @Override
    public Buffer getBuffer() {
        if (imageForLazyCustomConversion != null) {
            if (!((expectingEXTABGR && haveEXTABGR) ||
                  (expectingGL12    && haveGL12))) {
                revertPixelAttributes();
                // Must present the illusion to the end user that we are simply
                // wrapping the input BufferedImage
                createFromCustom(imageForLazyCustomConversion);
            }
        }
        return buffer;
    }

    private void createFromImage(GLProfile glp, BufferedImage image) {
        pixelAttributes = PixelAttributes.UNDEF; // Determine from image
        mustFlipVertically = true;

        width = image.getWidth();
        height = image.getHeight();

        int scanlineStride;

        SampleModel sm = image.getRaster().getSampleModel();
        if (sm instanceof SinglePixelPackedSampleModel) {
            scanlineStride =
                ((SinglePixelPackedSampleModel)sm).getScanlineStride();
        } else if (sm instanceof MultiPixelPackedSampleModel) {
            scanlineStride =
                ((MultiPixelPackedSampleModel)sm).getScanlineStride();
        } else if (sm instanceof ComponentSampleModel) {
            scanlineStride =
                ((ComponentSampleModel)sm).getScanlineStride();
        } else {
            // This will only happen for TYPE_CUSTOM anyway
            setupLazyCustomConversion(image);
            return;
        }

        width = image.getWidth();
        height = image.getHeight();

        if (glp.isGL2GL3()) {
            switch (image.getType()) {
                case BufferedImage.TYPE_INT_RGB:
                    pixelAttributes = new PixelAttributes(GL.GL_BGRA, GL2GL3.GL_UNSIGNED_INT_8_8_8_8_REV);
                    rowLength = scanlineStride;
                    alignment = 4;
                    expectingGL12 = true;
                    setupLazyCustomConversion(image);
                    break;
                case BufferedImage.TYPE_INT_ARGB_PRE:
                    pixelAttributes = new PixelAttributes(GL.GL_BGRA, GL2GL3.GL_UNSIGNED_INT_8_8_8_8_REV);
                    rowLength = scanlineStride;
                    alignment = 4;
                    expectingGL12 = true;
                    setupLazyCustomConversion(image);
                    break;
                case BufferedImage.TYPE_INT_BGR:
                    pixelAttributes = new PixelAttributes(GL.GL_RGBA, GL2GL3.GL_UNSIGNED_INT_8_8_8_8_REV);
                    rowLength = scanlineStride;
                    alignment = 4;
                    expectingGL12 = true;
                    setupLazyCustomConversion(image);
                    break;
                case BufferedImage.TYPE_3BYTE_BGR:
                    {
                        // we can pass the image data directly to OpenGL only if
                        // we have an integral number of pixels in each scanline
                        if ((scanlineStride % 3) == 0) {
                            pixelAttributes = new PixelAttributes(GL2GL3.GL_BGR, GL.GL_UNSIGNED_BYTE);
                            rowLength = scanlineStride / 3;
                            alignment = 1;
                        } else {
                            setupLazyCustomConversion(image);
                            return;
                        }
                    }
                    break;
                case BufferedImage.TYPE_4BYTE_ABGR_PRE:
                    {
                        // we can pass the image data directly to OpenGL only if
                        // we have an integral number of pixels in each scanline
                        // and only if the GL_EXT_abgr extension is present
    
                        // NOTE: disabling this code path for now as it appears it's
                        // buggy at least on some NVidia drivers and doesn't perform
                        // the necessary byte swapping (FIXME: needs more
                        // investigation)
                        if ((scanlineStride % 4) == 0 && glp.isGL2() && false) {
                            pixelAttributes = new PixelAttributes(GL2.GL_ABGR_EXT, GL.GL_UNSIGNED_BYTE);
                            rowLength = scanlineStride / 4;
                            alignment = 4;
    
                            // Store a reference to the original image for later in
                            // case it turns out that we don't have GL_EXT_abgr at the
                            // time we're going to do the texture upload to OpenGL
                            setupLazyCustomConversion(image);
                            expectingEXTABGR = true;
                            break;
                        } else {
                            setupLazyCustomConversion(image);
                            return;
                        }
                    }
                case BufferedImage.TYPE_USHORT_565_RGB:
                    pixelAttributes = new PixelAttributes(GL.GL_RGB, GL.GL_UNSIGNED_SHORT_5_6_5);
                    rowLength = scanlineStride;
                    alignment = 2;
                    expectingGL12 = true;
                    setupLazyCustomConversion(image);
                    break;
                case BufferedImage.TYPE_USHORT_555_RGB:
                    pixelAttributes = new PixelAttributes(GL.GL_BGRA, GL2GL3.GL_UNSIGNED_SHORT_1_5_5_5_REV);
                    rowLength = scanlineStride;
                    alignment = 2;
                    expectingGL12 = true;
                    setupLazyCustomConversion(image);
                    break;
                case BufferedImage.TYPE_BYTE_GRAY:
                    pixelAttributes = new PixelAttributes(GL.GL_LUMINANCE, GL.GL_UNSIGNED_BYTE);
                    rowLength = scanlineStride;
                    alignment = 1;
                    break;
                case BufferedImage.TYPE_USHORT_GRAY:
                    pixelAttributes = new PixelAttributes(GL.GL_LUMINANCE, GL.GL_UNSIGNED_SHORT);
                    rowLength = scanlineStride;
                    alignment = 2;
                    break;
                    // Note: TYPE_INT_ARGB and TYPE_4BYTE_ABGR images go down the
                    // custom code path to satisfy the invariant that images with an
                    // alpha channel always go down with premultiplied alpha.
                case BufferedImage.TYPE_INT_ARGB:
                case BufferedImage.TYPE_4BYTE_ABGR:
                case BufferedImage.TYPE_BYTE_BINARY:
                case BufferedImage.TYPE_BYTE_INDEXED:
                case BufferedImage.TYPE_CUSTOM:
                default:
                    java.awt.image.ColorModel cm = image.getColorModel();
                    if (cm.equals(rgbColorModel)) {
                        pixelAttributes = new PixelAttributes(GL.GL_RGB, GL.GL_UNSIGNED_BYTE);
                        rowLength = scanlineStride / 3;
                        alignment = 1;
                    } else if (cm.equals(rgbaColorModel)) {
                        pixelAttributes = new PixelAttributes(GL.GL_RGBA, GL.GL_UNSIGNED_BYTE);
                        rowLength = scanlineStride / 4; // FIXME: correct?
                        alignment = 4;
                    } else {
                        setupLazyCustomConversion(image);
                        return;
                    }
                    break;
            }
        } else {
            switch (image.getType()) {
                case BufferedImage.TYPE_INT_RGB:
                    pixelAttributes = new PixelAttributes(GL.GL_RGB, GL.GL_UNSIGNED_BYTE);
                    rowLength = scanlineStride;
                    alignment = 3;
                    expectingGL12 = true;
                    setupLazyCustomConversion(image);
                    break;
                case BufferedImage.TYPE_INT_ARGB_PRE:
                    throw new GLException("INT_ARGB_PRE n.a.");
                case BufferedImage.TYPE_INT_BGR:
                    throw new GLException("INT_BGR n.a.");
                case BufferedImage.TYPE_3BYTE_BGR:
                    throw new GLException("INT_BGR n.a.");
                case BufferedImage.TYPE_4BYTE_ABGR_PRE:
                    throw new GLException("INT_BGR n.a.");
                case BufferedImage.TYPE_USHORT_565_RGB:
                    pixelAttributes = new PixelAttributes(GL.GL_RGB, GL.GL_UNSIGNED_SHORT_5_6_5);
                    rowLength = scanlineStride;
                    alignment = 2;
                    expectingGL12 = true;
                    setupLazyCustomConversion(image);
                    break;
                case BufferedImage.TYPE_USHORT_555_RGB:
                    pixelAttributes = new PixelAttributes(GL.GL_RGBA, GL.GL_UNSIGNED_SHORT_5_5_5_1);
                    rowLength = scanlineStride;
                    alignment = 2;
                    expectingGL12 = true;
                    setupLazyCustomConversion(image);
                    break;
                case BufferedImage.TYPE_BYTE_GRAY:
                    pixelAttributes = new PixelAttributes(GL.GL_LUMINANCE, GL.GL_UNSIGNED_BYTE);
                    rowLength = scanlineStride;
                    alignment = 1;
                    break;
                case BufferedImage.TYPE_USHORT_GRAY:
                    throw new GLException("USHORT_GRAY n.a.");
                    // Note: TYPE_INT_ARGB and TYPE_4BYTE_ABGR images go down the
                    // custom code path to satisfy the invariant that images with an
                    // alpha channel always go down with premultiplied alpha.
                case BufferedImage.TYPE_INT_ARGB:
                case BufferedImage.TYPE_4BYTE_ABGR:
                case BufferedImage.TYPE_BYTE_BINARY:
                case BufferedImage.TYPE_BYTE_INDEXED:
                case BufferedImage.TYPE_CUSTOM:
                default:
                    java.awt.image.ColorModel cm = image.getColorModel();
                    if (cm.equals(rgbColorModel)) {
                        pixelAttributes = new PixelAttributes(GL.GL_RGB, GL.GL_UNSIGNED_BYTE);
                        rowLength = scanlineStride / 3;
                        alignment = 1;
                    } else if (cm.equals(rgbaColorModel)) {
                        pixelAttributes = new PixelAttributes(GL.GL_RGBA, GL.GL_UNSIGNED_BYTE);
                        rowLength = scanlineStride / 4; // FIXME: correct?
                        alignment = 4;
                    } else {
                        setupLazyCustomConversion(image);
                        return;
                    }
                    break;
            }
        }

        createNIOBufferFromImage(image);
    }

    private void setupLazyCustomConversion(BufferedImage image) {
        imageForLazyCustomConversion = image;
        boolean hasAlpha = image.getColorModel().hasAlpha();
        int pixelFormat = pixelAttributes.format;
        int pixelType = pixelAttributes.type;
        if (pixelFormat == 0) {
            pixelFormat = hasAlpha ? GL.GL_RGBA : GL.GL_RGB;
        }
        alignment = 1; // FIXME: do we need better?
        rowLength = width; // FIXME: correct in all cases?

        // Allow previously-selected pixelType (if any) to override that
        // we can infer from the DataBuffer
        DataBuffer data = image.getRaster().getDataBuffer();
        if (data instanceof DataBufferByte || isPackedInt(image)) {
            // Don't use GL_UNSIGNED_INT for BufferedImage packed int images
            if (pixelType == 0) pixelType = GL.GL_UNSIGNED_BYTE;
        } else if (data instanceof DataBufferDouble) {
            throw new RuntimeException("DataBufferDouble rasters not supported by OpenGL");
        } else if (data instanceof DataBufferFloat) {
            if (pixelType == 0) pixelType = GL.GL_FLOAT;
        } else if (data instanceof DataBufferInt) {
            // FIXME: should we support signed ints?
            if (pixelType == 0) pixelType = GL2GL3.GL_UNSIGNED_INT;
        } else if (data instanceof DataBufferShort) {
            if (pixelType == 0) pixelType = GL.GL_SHORT;
        } else if (data instanceof DataBufferUShort) {
            if (pixelType == 0) pixelType = GL.GL_UNSIGNED_SHORT;
        } else {
            throw new RuntimeException("Unexpected DataBuffer type?");
        }
        pixelAttributes = new PixelAttributes(pixelFormat, pixelType);
    }

    private void createFromCustom(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        // create a temporary image that is compatible with OpenGL
        boolean hasAlpha = image.getColorModel().hasAlpha();
        java.awt.image.ColorModel cm = null;
        int dataBufferType = image.getRaster().getDataBuffer().getDataType();
        // Don't use integer components for packed int images
        if (isPackedInt(image)) {
            dataBufferType = DataBuffer.TYPE_BYTE;
        }
        if (dataBufferType == DataBuffer.TYPE_BYTE) {
            cm = hasAlpha ? rgbaColorModel : rgbColorModel;
        } else {
            if (hasAlpha) {
                cm = new ComponentColorModel(java.awt.color.ColorSpace.getInstance(java.awt.color.ColorSpace.CS_sRGB),
                                             null, true, true,
                                             Transparency.TRANSLUCENT,
                                             dataBufferType);
            } else {
                cm = new ComponentColorModel(java.awt.color.ColorSpace.getInstance(java.awt.color.ColorSpace.CS_sRGB),
                                             null, false, false,
                                             Transparency.OPAQUE,
                                             dataBufferType);
            }
        }

        boolean premult = cm.isAlphaPremultiplied();
        WritableRaster raster =
            cm.createCompatibleWritableRaster(width, height);
        BufferedImage texImage = new BufferedImage(cm, raster, premult, null);

        // copy the source image into the temporary image
        Graphics2D g = texImage.createGraphics();
        g.setComposite(AlphaComposite.Src);
        g.drawImage(image, 0, 0, null);
        g.dispose();

        // Wrap the buffer from the temporary image
        createNIOBufferFromImage(texImage);
    }

    private boolean isPackedInt(BufferedImage image) {
        int imgType = image.getType();
        return (imgType == BufferedImage.TYPE_INT_RGB ||
                imgType == BufferedImage.TYPE_INT_BGR ||
                imgType == BufferedImage.TYPE_INT_ARGB ||
                imgType == BufferedImage.TYPE_INT_ARGB_PRE);
    }

    private void revertPixelAttributes() {
        // Knowing we don't have e.g. OpenGL 1.2 functionality available,
        // and knowing we're in the process of doing the fallback code
        // path, re-infer a vanilla pixel format and type compatible with
        // OpenGL 1.1
        pixelAttributes = PixelAttributes.UNDEF;
        setupLazyCustomConversion(imageForLazyCustomConversion);
    }

    private void createNIOBufferFromImage(BufferedImage image) {
        buffer = wrapImageDataBuffer(image);
    }

    private Buffer wrapImageDataBuffer(BufferedImage image) {
        //
        // Note: Grabbing the DataBuffer will defeat Java2D's image
        // management mechanism (as of JDK 5/6, at least).  This shouldn't
        // be a problem for most JOGL apps, but those that try to upload
        // the image into an OpenGL texture and then use the same image in
        // Java2D rendering might find the 2D rendering is not as fast as
        // it could be.
        //

        DataBuffer data = image.getRaster().getDataBuffer();
        if (data instanceof DataBufferByte) {
            return ByteBuffer.wrap(((DataBufferByte) data).getData());
        } else if (data instanceof DataBufferDouble) {
            throw new RuntimeException("DataBufferDouble rasters not supported by OpenGL");
        } else if (data instanceof DataBufferFloat) {
            return FloatBuffer.wrap(((DataBufferFloat) data).getData());
        } else if (data instanceof DataBufferInt) {
            return IntBuffer.wrap(((DataBufferInt) data).getData());
        } else if (data instanceof DataBufferShort) {
            return ShortBuffer.wrap(((DataBufferShort) data).getData());
        } else if (data instanceof DataBufferUShort) {
            return ShortBuffer.wrap(((DataBufferUShort) data).getData());
        } else {
            throw new RuntimeException("Unexpected DataBuffer type?");
        }
    }
}
