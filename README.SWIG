ANDROID:
=======

export NDK=/home/mab/src/external/android_sdk/android-ndk-r8b/
export SYSROOT=$NDK/platforms/android-8/arch-arm/
export CROSS_COMPILE=arm-linux-androideabi
export CC=${CROSS_COMPILE}-gcc
export CXX=${CROSS_COMPILE}-g++
export CFLAGS="--sysroot=$SYSROOT  -I$SYSROOT/usr/include"
export CXXFLAGS="--sysroot=$SYSROOT -I$SYSROOT/usr/include"
export CPPFLAGS="--sysroot=$SYSROOT -I$SYSROOT/usr/include"

PATH=$PATH:$NDK/toolchains/arm-linux-androideabi-4.4.3/prebuilt/linux-x86/bin/

./configure --with-swig --build=x86-unknown-linux-gnu --host=arm-linux-androideabi --target=arm-linux-androideabi

make java
