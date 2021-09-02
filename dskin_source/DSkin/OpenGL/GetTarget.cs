﻿namespace DSkin.OpenGL
{
    using System;

    public enum GetTarget : uint
    {
        AccumAlphaBits = 0xd5b,
        AccumBlueBits = 0xd5a,
        AccumClearValue = 0xb80,
        AccumGreenBits = 0xd59,
        AccumRedBits = 0xd58,
        AlphaBias = 0xd1d,
        AlphaBits = 0xd55,
        AlphaScale = 0xd1c,
        AlphaTest = 0xbc0,
        AlphaTestFunc = 0xbc1,
        AlphaTestRef = 0xbc2,
        AttribStackDepth = 0xbb0,
        AutoNormal = 0xd80,
        AuxBuffers = 0xc00,
        Blend = 0xbe2,
        BlendDst = 0xbe0,
        BlendSrc = 0xbe1,
        BlueBias = 0xd1b,
        BlueBits = 0xd54,
        BlueScale = 0xd1a,
        ClientAttribStackDepth = 0xbb1,
        ColorClearValue = 0xc22,
        ColorLogicOp = 0xbf2,
        ColorMaterial = 0xb57,
        ColorMaterialFace = 0xb55,
        ColorMaterialParameter = 0xb56,
        ColorWritemask = 0xc23,
        const_180 = 0xd90,
        const_181 = 0xd91,
        const_182 = 0xd92,
        const_187 = 0xd97,
        const_188 = 0xd98,
        const_189 = 0xdb0,
        const_190 = 0xdb1,
        const_191 = 0xdb2,
        const_196 = 0xdb7,
        const_197 = 0xdb8,
        const_202 = 0xde0,
        const_203 = 0xde1,
        CullFace = 0xb44,
        CullFaceMode = 0xb45,
        CurrentColor = 0xb00,
        CurrentIndex = 0xb01,
        CurrentNormal = 0xb02,
        CurrentRasterColor = 0xb04,
        CurrentRasterDistance = 0xb09,
        CurrentRasterIndex = 0xb05,
        CurrentRasterPosition = 0xb07,
        CurrentRasterPositionValid = 0xb08,
        CurrentRasterTextureCoords = 0xb06,
        CurrentTextureCoords = 0xb03,
        DepthBias = 0xd1f,
        DepthBits = 0xd56,
        DepthClearValue = 0xb73,
        DepthFunc = 0xb74,
        DepthRange = 0xb70,
        DepthScale = 0xd1e,
        DepthTest = 0xb71,
        DepthWritemask = 0xb72,
        Dither = 0xbd0,
        DoubleBuffer = 0xc32,
        DrawBuffer = 0xc01,
        EdgeFlag = 0xb43,
        FeedbackBufferPointer = 0xdf0,
        FeedbackBufferSize = 0xdf1,
        FeedbackBufferType = 0xdf2,
        Fog = 0xb60,
        FogColor = 0xb66,
        FogDensity = 0xb62,
        FogEnd = 0xb64,
        FogHint = 0xc54,
        FogIndex = 0xb61,
        FogMode = 0xb65,
        FogStart = 0xb63,
        FrontFace = 0xb46,
        GreenBias = 0xd19,
        GreenBits = 0xd53,
        GreenScale = 0xd18,
        IndexBits = 0xd51,
        IndexClearValue = 0xc20,
        IndexLogicOp = 0xbf1,
        IndexMode = 0xc30,
        IndexOffset = 0xd13,
        IndexShift = 0xd12,
        IndexWritemask = 0xc21,
        Lighting = 0xb50,
        LightModelAmbient = 0xb53,
        LightModelLocalViewer = 0xb51,
        LightModelTwoSide = 0xb52,
        LineSmooth = 0xb20,
        LineSmoothHint = 0xc52,
        LineStipple = 0xb24,
        LineStipplePattern = 0xb25,
        LineStippleRepeat = 0xb26,
        LineWidth = 0xb21,
        LineWidthGranularity = 0xb23,
        LineWidthRange = 0xb22,
        ListBase = 0xb32,
        ListIndex = 0xb33,
        ListMode = 0xb30,
        LogicOpMode = 0xbf0,
        LsbFirst = 0xcf1,
        Map1GridDomain = 0xdd0,
        Map1GridSegments = 0xdd1,
        Map1TextureCoord1 = 0xd93,
        Map1TextureCoord2 = 0xd94,
        Map1TextureCoord3 = 0xd95,
        Map1TextureCoord4 = 0xd96,
        Map2GridDomain = 0xdd2,
        Map2GridSegments = 0xdd3,
        Map2TextureCoord1 = 0xdb3,
        Map2TextureCoord2 = 0xdb4,
        Map2TextureCoord3 = 0xdb5,
        Map2TextureCoord4 = 0xdb6,
        MapColor = 0xd10,
        MapEvalOrder = 0xd30,
        MapPixelMapTable = 0xd34,
        MapStencil = 0xd11,
        MatrixMode = 0xba0,
        MaxAttribStackDepth = 0xd35,
        MaxClientAttribStackDepth = 0xd3b,
        MaxClipPlanes = 0xd32,
        MaxLights = 0xd31,
        MaxListNesting = 0xb31,
        MaxModelviewStackDepth = 0xd36,
        MaxNameStackDepth = 0xd37,
        MaxProjectionStackDepth = 0xd38,
        MaxTextureSize = 0xd33,
        MaxTextureStackDepth = 0xd39,
        MaxViewportDims = 0xd3a,
        ModelviewMatix = 0xba6,
        ModelviewStackDepth = 0xba3,
        NameStackDepth = 0xd70,
        Normalize = 0xba1,
        PackAlignment = 0xd05,
        PackLsbFirst = 0xd01,
        PackRowLength = 0xd02,
        PackSkipPixels = 0xd04,
        PackSkipRows = 0xd03,
        PackSwapBytes = 0xd00,
        PerspectiveCorrectionHint = 0xc50,
        PixelMapAtoA = 0xc79,
        PixelMapAtoASize = 0xcb9,
        PixelMapBtoB = 0xc78,
        PixelMapBtoBSize = 0xcb8,
        PixelMapGtoG = 0xc77,
        PixelMapGtoGSize = 0xcb7,
        PixelMapItoA = 0xc75,
        PixelMapItoASize = 0xcb5,
        PixelMapItoB = 0xc74,
        PixelMapItoBSize = 0xcb4,
        PixelMapItoG = 0xc73,
        PixelMapItoGSize = 0xcb3,
        PixelMapItoI = 0xc70,
        PixelMapItoISize = 0xcb0,
        PixelMapItoR = 0xc72,
        PixelMapItoRSize = 0xcb2,
        PixelMapRtoR = 0xc76,
        PixelMapRtoRSize = 0xcb6,
        PixelMapStoS = 0xc71,
        PixelMapStoSSize = 0xcb1,
        PointSize = 0xb11,
        PointSizeGranularity = 0xb13,
        PointSizeRange = 0xb12,
        PointSmooth = 0xb10,
        PointSmoothHint = 0xc51,
        PolygonMode = 0xb40,
        PolygonSmooth = 0xb41,
        PolygonSmoothHint = 0xc53,
        PolygonStipple = 0xb42,
        ProjectionMatrix = 0xba7,
        ProjectionStackDepth = 0xba4,
        ReadBuffer = 0xc02,
        RedBias = 0xd15,
        RedBits = 0xd52,
        RedScale = 0xd14,
        RenderMode = 0xc40,
        RgbaMode = 0xc31,
        ScissorBox = 0xc10,
        ScissorTest = 0xc11,
        SelectionBufferPointer = 0xdf3,
        SelectionBufferSize = 0xdf4,
        ShadeModel = 0xb54,
        StencilBits = 0xd57,
        StencilClearValue = 0xb91,
        StencilFail = 0xb94,
        StencilFunc = 0xb92,
        StencilPassDepthFail = 0xb95,
        StencilPassDepthPass = 0xb96,
        StencilRef = 0xb97,
        StencilTest = 0xb90,
        StencilValueMask = 0xb93,
        StencilWritemask = 0xb98,
        Stereo = 0xc33,
        SubpixelBits = 0xd50,
        TextureGenQ = 0xc63,
        TextureGenR = 0xc62,
        TextureGenS = 0xc60,
        TextureGenT = 0xc61,
        TextureMatrix = 0xba8,
        TextureStackDepth = 0xba5,
        UnpackAlignment = 0xcf5,
        UnpackRowLength = 0xcf2,
        UnpackSkipPixels = 0xcf4,
        UnpackSkipRows = 0xcf3,
        UnpackSwapBytes = 0xcf0,
        Viewport = 0xba2,
        ZoomX = 0xd16,
        ZoomY = 0xd17
    }
}

