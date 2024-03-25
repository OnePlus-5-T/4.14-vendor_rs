#!/bin/bash

# Get current cpu cores number and set it as default number of threads for make
CPUS=$(nproc)

cd $ANDROID_BUILD_TOP

m installclean -j$CPUS

if [ $? != 0 ]; then
    echo "installclean failed"
    exit 1
fi

m -j$CPUS

if [ $? != 0 ]; then
    echo "make failed"
    exit 1
fi

rm -rf $OUT/ota_custom

mkdir $OUT/ota_custom
cp -r $ANDROID_BUILD_TOP/vendor/rs/ota_build/$TARGET_PRODUCT/ota_sample/* $OUT/ota_custom/

if [ $? != 0 ]; then
    echo "Cannot copy sample packages to out directory"
    exit 1
fi

cd $OUT

files=(
    "ramdisk/system/etc/ramdisk/build.prop"
    "system/build.prop"
    "system/product/etc/build.prop"
    "system/system_dlkm/etc/build.prop"
    "system/system_ext/etc/build.prop"
    "vendor/vendor_dlkm/etc/build.prop"
    "vendor/odm_dlkm/etc/build.prop"
    "vendor/odm/etc/build.prop"
    "vendor/build.prop"
)

for file in "${files[@]}"; do
    sed -i 's/test-keys/release-keys/g' "$file"
done

m vnod -j$CPUS

if [ $? != 0 ]; then
    echo "vnod failed"
    exit 1
fi

m snod -j$CPUS

if [ $? != 0 ]; then
    echo "snod failed"
    exit 1
fi

m bootimage -j$CPUS

if [ $? != 0 ]; then
    echo "make bootimage failed"
    exit 1
fi

simg2img system.img system.raw

if [ $? != 0 ]; then
    echo "simg2img system failed"
    exit 1
fi

simg2img vendor.img vendor.raw

if [ $? != 0 ]; then
    echo "simg2img vendor failed"
    exit 1
fi

mv system.raw $OUT/ota_custom/system.img
mv vendor.raw $OUT/ota_custom/vendor.img
cp boot.img $OUT/ota_custom/boot.img

cd $OUT/ota_custom
zip -r ${TARGET_PRODUCT}_ota.zip *

if [ $? != 0 ]; then
    echo "Cannot compress final zip"
    exit 1
fi

cd $OUT
java -jar -Djava.library.path="$ANDROID_BUILD_TOP/out/host/linux-x86/lib64" $ANDROID_BUILD_TOP/out/host/linux-x86/framework/signapk.jar -w $ANDROID_BUILD_TOP/vendor/rs/config/security/releasekey.x509.pem $ANDROID_BUILD_TOP/vendor/rs/config/security/releasekey.pk8 $OUT/ota_custom/${TARGET_PRODUCT}_ota.zip $OUT/${TARGET_PRODUCT}_ota.zip

if [ $? != 0 ]; then
    echo "Cannot sign final zip"
    exit 1
fi

rm -rf $OUT/ota_custom/${TARGET_PRODUCT}_ota.zip

cd $ANDROID_BUILD_TOP
