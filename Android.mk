LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-subdir-java-files)

LOCAL_STATIC_JAVA_LIBRARIES := \
    android-support-v4 \
    dkcolorpicker

LOCAL_JNI_SHARED_LIBRARIES := libjni_terminal

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res \
    external/dkcolorpicker/res

LOCAL_AAPT_FLAGS := \
    --auto-add-overlay \
    --extra-packages net.darkkatrom.dkcolorpicker

# TODO: enable proguard once development has settled down
#LOCAL_PROGUARD_FLAG_FILES := proguard.flags
LOCAL_PROGUARD_ENABLED := disabled

LOCAL_PACKAGE_NAME := Terminal

include $(BUILD_PACKAGE)

include $(call all-makefiles-under,$(LOCAL_PATH))
