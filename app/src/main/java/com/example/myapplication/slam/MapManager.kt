package com.example.myapplication.slam

import com.google.ar.core.Anchor
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.core.TrackingState

class MapManager {

    private val anchors = mutableListOf<Anchor>()

    /**
     * Updates the 3D map with new feature points from the current frame.
     *
     * @param session The ARCore session.
     * @param frame The current frame.
     */
    fun updateMap(session: Session, frame: Frame) {
        if (frame.camera.trackingState == TrackingState.TRACKING) {
            val imageIntrinsics = frame.camera.imageIntrinsics
            val imageDimensions = imageIntrinsics.imageDimensions
            frame.hitTest(imageDimensions[0] / 2f, imageDimensions[1] / 2f).forEach {
                val anchor = session.createAnchor(it.hitPose)
                anchors.add(anchor)
            }
        }
    }

    /**
     * Returns the current list of anchors in the map.
     *
     * @return The list of anchors.
     */
    fun getAnchors(): List<Anchor> {
        return anchors
    }
}
