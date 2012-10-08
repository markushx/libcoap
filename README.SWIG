PATH=$PATH:/home/mab/src/external/android_sdk/android-ndk-r6/toolchains/arm-linux-androideabi-4.4.3/prebuilt/linux-x86/bin/ \
CC=/home/mab/src/external/android_sdk/android-ndk-r6/toolchains/arm-linux-androideabi-4.4.3/prebuilt/linux-x86/bin/arm-linux-androideabi-gcc \
CFLAGS="--sysroot=/home/mab/src/external/android_sdk/android-ndk-r6/platforms/android-8/arch-arm/" \
./configure --with-swig --build=x86-unknown-linux-gnu --host=arm-linux-androideabi --target=arm-linux-androideabi

make java
