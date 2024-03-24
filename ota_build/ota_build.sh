#!/bin/bash

cd $ANDROID_BUILD_TOP

m installclean -j8
m -j8

rm -rf $OUT/ota_custom
mkdir $OUT/ota_custom
cp -r $ANDROID_BUILD_TOP/vendor/rs/ota_build/$TARGET_PRODUCT/ota_sample/* $OUT/ota_custom/
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

m vnod -j8
m snod -j8
m bootimage -j8
simg2img system.img system.raw
simg2img vendor.img vendor.raw
mv system.raw $OUT/ota_custom/system.img
mv vendor.raw $OUT/ota_custom/vendor.img
cp boot.img $OUT/ota_custom/boot.img

cd $OUT/ota_custom
zip -r ${TARGET_PRODUCT}_ota.zip *
cd $OUT
java -jar -Djava.library.path="$ANDROID_BUILD_TOP/out/host/linux-x86/lib64" $ANDROID_BUILD_TOP/out/host/linux-x86/framework/signapk.jar -w $ANDROID_BUILD_TOP/vendor/rs/config/security/releasekey.x509.pem $ANDROID_BUILD_TOP/vendor/rs/config/security/releasekey.pk8 $OUT/ota_custom/${TARGET_PRODUCT}_ota.zip $OUT/${TARGET_PRODUCT}_ota.zip

rm -rf $OUT/ota_custom/${TARGET_PRODUCT}_ota.zip

cd $ANDROID_BUILD_TOP