# Copyright (C) 2009 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

#include for in_port_t ??
#LOCAL_C_INCLUDES      := $(TARGET_C_INCLUDES)/linux $(TARGET_C_INCLUDES)/sys
#LOCAL_EXPORT_CFLAGS   := -NDEBUG=1

LOCAL_CFLAGS          := -DJAVA -DANDROID
#LOCAL_CFLAGS          := -nostdlib -DANDROID_NDK
# -fno-short-wchar -fno-strength-reduce -fno-strict-aliasing
# -fshort-wchar -Wno-multichar -D_ANDROID
#LOCAL_LDLIBS          := -lc -llog
LOCAL_LDLIBS := -llog

LOCAL_MODULE          := coap
#LOCAL_MODULE_FILENAME := libcoap

LOCAL_SRC_FILES       := async.c block.c coap_list.c debug.c encode.c hashkey.c  net.c option.c pdu.c resource.c str.c subscribe.c uri.c coap_wrap.c

APP_STL := gnustl_static

include $(BUILD_SHARED_LIBRARY)
