package com.ankitrawatgit.iqoo9rootghostlock

object NativeProbe {
    init {
        System.loadLibrary("ghostlock_native")
    }

    external fun run(): String

    external fun isKernelSuActive(): Boolean
}
