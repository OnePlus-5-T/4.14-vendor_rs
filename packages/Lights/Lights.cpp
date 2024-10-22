/*
 * Copyright (C) 2021 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#include "Lights.h"

#include <android-base/logging.h>
#include "LED.h"
#include "Utils.h"

#define _REALLY_INCLUDE_SYS__SYSTEM_PROPERTIES_H_
#include <sys/_system_properties.h>

namespace aidl {
namespace android {
namespace hardware {
namespace light {

static const std::string kAllBacklightPaths[] = {
    "/sys/class/backlight/panel0-backlight/brightness",
    "/sys/class/leds/lcd-backlight/brightness",
};

static const std::string kMaxBacklightPath = "/sys/class/leds/lcd-backlight/max_brightness";

enum led_type {
    RED,
    GREEN,
    BLUE,
    WHITE,
    MAX_LEDS,
};

static LED kLEDs[MAX_LEDS] = {
    [RED] = LED("red"),
    [GREEN] = LED("green"),
    [BLUE] = LED("blue"),
    [WHITE] = LED("white"),
};

#define AutoHwLight(light) {.id = (int32_t)light, .type = light, .ordinal = 0}

static const HwLight kBacklightHwLight = AutoHwLight(LightType::BACKLIGHT);
static const HwLight kBatteryHwLight = AutoHwLight(LightType::BATTERY);
static const HwLight kNotificationHwLight = AutoHwLight(LightType::NOTIFICATIONS);

Lights::Lights() {
    for (auto& backlight : kAllBacklightPaths) {
        if (!fileWriteable(backlight))
            continue;

        mBacklightPath = backlight;
        mLights.push_back(kBacklightHwLight);
        break;
    }

    mWhiteLED = kLEDs[WHITE].exists();

    mLights.push_back(kBatteryHwLight);
    mLights.push_back(kNotificationHwLight);
}

ndk::ScopedAStatus Lights::setLightState(int32_t id, const HwLightState& state) {
    LightType type = static_cast<LightType>(id);
    switch (type) {
        case LightType::BACKLIGHT:
            if (!mBacklightPath.empty()) {
                uint32_t newBrightness = colorToBrightness(state.color);
                uint32_t maxBrightness = 255;
                readFromFile(kMaxBacklightPath, &maxBrightness);
                // If max panel brightness exceeds 255, apply proper linear scaling
                if (newBrightness > 0 && maxBrightness != 255) {
                    int originalBrightness = newBrightness;
                    newBrightness = (((maxBrightness - 1) * (newBrightness - 1)) / (254)) + 1;
                    LOG(DEBUG) << "Scaling backlight brightness from " << originalBrightness << " => " << newBrightness;
                }
                writeToFile(mBacklightPath, newBrightness);
            };
            break;
        case LightType::BATTERY:
        case LightType::NOTIFICATIONS:
            mLEDMutex.lock();
            if (type == LightType::BATTERY)
                mLastBatteryState = state;
            else
                mLastNotificationState = state;
            setLED(isLit(mLastBatteryState.color) ? mLastBatteryState : mLastNotificationState);
            mLEDMutex.unlock();
            break;
        default:
            return ndk::ScopedAStatus::fromExceptionCode(EX_UNSUPPORTED_OPERATION);
            break;
    }

    return ndk::ScopedAStatus::ok();
}

ndk::ScopedAStatus Lights::getLights(std::vector<HwLight> *_aidl_return) {
    for (auto& light : mLights)
        _aidl_return->push_back(light);

    return ndk::ScopedAStatus::ok();
}

void Lights::setLED(const HwLightState& state) {
    bool rc = true;
    argb_t color = colorToArgb(state.color);
    uint32_t blink = (state.flashOnMs != 0 && state.flashOffMs != 0);

    char value[1] = {0};
    int len = __system_property_get("persist.sys.disable.rgb", value);
    if (len > 0) {
        return;
    }

    switch (state.flashMode) {
        case FlashMode::HARDWARE:
        case FlashMode::TIMED:
            if (mWhiteLED) {
                rc = kLEDs[WHITE].setBreath(blink);
            } else {
                if (!!color.red)
                    rc &= kLEDs[RED].setBreath(blink);
                if (!!color.green)
                    rc &= kLEDs[GREEN].setBreath(blink);
                if (!!color.blue)
                    rc &= kLEDs[BLUE].setBreath(blink);
            }
            if (rc)
                break;
            FALLTHROUGH_INTENDED;
        default:
            if (mWhiteLED) {
                rc = kLEDs[WHITE].setBrightness(colorToBrightness(state.color));
            } else {
                rc = kLEDs[RED].setBrightness(color.red);
                rc &= kLEDs[GREEN].setBrightness(color.green);
                rc &= kLEDs[BLUE].setBrightness(color.blue);
            }
            break;
    }

    return;
}

} // namespace light
} // namespace hardware
} // namespace android
} // namespace aidl
