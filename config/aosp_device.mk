# RIL Wrapper
PRODUCT_PACKAGES += \
    libril-wrapper

# Extra Apps
PRODUCT_PACKAGES += \
    SettingsExtra \
    LedManagerExtra

# Theme picker
PRODUCT_PACKAGES += \
    ThemePicker \
    ThemesStub

# Theme overlays
PRODUCT_PACKAGES += \
    IconShapeCircleOverlay

PRODUCT_PACKAGES += \
    messaging

# Custom light HAL
PRODUCT_PACKAGES += \
    light_daemon

# Quick Access Wallet
PRODUCT_PACKAGES += \
    QuickAccessWallet

# Netd
PRODUCT_PACKAGES += \
    android.system.net.netd@1.1.vendor

# Bluetooth
PRODUCT_PACKAGES += \
    android.hardware.bluetooth@1.0.vendor \
    android.hardware.bluetooth@1.1.vendor

# DRM
PRODUCT_PACKAGES += \
    android.hardware.drm@1.4.vendor

# Special permissions for system apps
PRODUCT_PACKAGES += \
    aosp-sysconfig.xml

# SimpleDeviceConfig
PRODUCT_PACKAGES += \
    SimpleDeviceConfig

# Launcher3Overlay
PRODUCT_PACKAGES += \
    Launcher3Overlay

# Animation when charging from powered off
PRODUCT_PACKAGES += \
    lineage_charger_animation

# Workaround for prebuilt Qualcomm neural network HAL
PRODUCT_PACKAGES += \
    libprotobuf-cpp-full-3.9.1-vendorcompat

# Vendor dependencies
PRODUCT_PACKAGES += \
    libpower.vendor \
    libhidlmemory.vendor \
    libsqlite.vendor

# Open Camera
PRODUCT_PACKAGES += \
    OpenCamera

# SimpleKeyboard
PRODUCT_PACKAGES += \
    SimpleKeyboard

# Dexpreopt
PRODUCT_DEXPREOPT_SPEED_APPS += \
    SystemUI

PRODUCT_SYSTEM_DEFAULT_PROPERTIES += \
    dalvik.vm.systemuicompilerfilter=speed

PRODUCT_ENFORCE_RRO_TARGETS := framework-res

PRODUCT_VENDOR_KERNEL_HEADERS := vendor/rs/kernel-headers

PRODUCT_SOONG_NAMESPACES += \
    vendor/qcom/opensource/data-ipa-cfg-mgr \
    vendor/qcom/opensource/dataservices \
    vendor/rs/config \
    hardware/qcom/wlan/legacy

# Camera API1 ZSL
PRODUCT_PROPERTY_OVERRIDES += \
    camera.disable_zsl_mode=1

# XML format
PRODUCT_PROPERTY_OVERRIDES += \
    persist.sys.binary_xml=false

# Disable remote keyguard animation
PRODUCT_SYSTEM_DEFAULT_PROPERTIES += \
    persist.wm.enable_remote_keyguard_animation=0

PRODUCT_BROKEN_VERIFY_USES_LIBRARIES := true
