#!/usr/bin/env bash
# Copyright 2021 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

set -ex

if [ "${PLATFORM}" == "jenkins" ]; then
  bazelrc=--bazelrc=bazelrc.remote
fi
BAZEL_DIRS=${HOME}/.bazel-dirs
BAZEL_ARGUMENTS="--show_timestamps --announce_rc --experimental_convenience_symlinks=normal --symlink_prefix=${BAZEL_DIRS}/"
bazel ${bazelrc} build ${BAZEL_ARGUMENTS}
cat ${BAZEL_DIRS}/out/stable-status.txt
cat ${BAZEL_DIRS}/out/volatile-status.txt

ACCESS_CONTROL_MODULE="//access-control/service:module"
bazel ${bazelrc} build $ACCESS_CONTROL_MODULE `bazel query "//...:*" | grep "module_deploy.jar"` ${BAZEL_ARGUMENTS} --remote_download_outputs=all