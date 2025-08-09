package com.example.myapplication.slam

import com.google.ar.core.Pose

class MotionEstimator {

    /**
     * Estimates the motion between two camera poses.
     *
     * @param currentPose The current camera pose.
     * @param previousPose The previous camera pose.
     * @return The transformation from the previous to the current pose.
     */
    fun estimateMotion(currentPose: Pose, previousPose: Pose): Pose {
        return previousPose.inverse().compose(currentPose)
    }
}
