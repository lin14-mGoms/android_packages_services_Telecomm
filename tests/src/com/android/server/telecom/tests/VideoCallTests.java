/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.server.telecom.tests;

import org.mockito.ArgumentCaptor;

import android.os.RemoteException;
import android.telecom.CallAudioState;
import android.telecom.VideoProfile;
import android.test.suitebuilder.annotation.MediumTest;

import com.android.server.telecom.CallAudioModeStateMachine;
import com.android.server.telecom.CallAudioRouteStateMachine;
import com.android.server.telecom.Log;

import java.util.List;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

/**
 * System tests for video-specific behavior in telecom.
 * TODO: Add unit tests which ensure that auto-speakerphone does not occur when using a wired
 * headset or a bluetooth headset.
 */
public class VideoCallTests extends TelecomSystemTest {

    /**
     * Tests to ensure an incoming video-call is automatically routed to the speakerphone when
     * the call is answered and neither a wired headset nor bluetooth headset are connected.
     */
    @MediumTest
    public void testAutoSpeakerphoneIncomingBidirectional() throws Exception {
        // Start an incoming video call.
        IdPair ids = startAndMakeActiveIncomingCall("650-555-1212",
                mPhoneAccountA0.getAccountHandle(), mConnectionServiceFixtureA,
                VideoProfile.STATE_BIDIRECTIONAL);

        verifyAudioRoute(CallAudioState.ROUTE_SPEAKER);
    }

    /**
     * Tests to ensure an incoming receive-only video-call is answered in speakerphone mode.  Note
     * that this is not a scenario we would expect normally with the default dialer as it will
     * always answer incoming video calls as bi-directional.  It is, however, possible for a third
     * party dialer to answer an incoming video call a a one-way video call.
     */
    @MediumTest
    public void testAutoSpeakerphoneIncomingReceiveOnly() throws Exception {
        // Start an incoming video call.
        IdPair ids = startAndMakeActiveIncomingCall("650-555-1212",
                mPhoneAccountA0.getAccountHandle(), mConnectionServiceFixtureA,
                VideoProfile.STATE_RX_ENABLED);

        verifyAudioRoute(CallAudioState.ROUTE_SPEAKER);
    }

    /**
     * Tests audio routing for an outgoing video call made with bidirectional video.  Expect to be
     * in speaker mode.
     */
    @MediumTest
    public void testAutoSpeakerphoneOutgoingBidirectional() throws Exception {
        // Start an incoming video call.
        IdPair ids = startAndMakeActiveOutgoingCall("650-555-1212",
                mPhoneAccountA0.getAccountHandle(), mConnectionServiceFixtureA,
                VideoProfile.STATE_BIDIRECTIONAL, null);

        verifyAudioRoute(CallAudioState.ROUTE_SPEAKER);
    }

    /**
     * Tests audio routing for an outgoing video call made with transmit only video.  Expect to be
     * in speaker mode.  Note: The default UI does not support making one-way video calls, but the
     * APIs do and a third party incall UI could choose to support that.
     */
    @MediumTest
    public void testAutoSpeakerphoneOutgoingTransmitOnly() throws Exception {
        // Start an incoming video call.
        IdPair ids = startAndMakeActiveOutgoingCall("650-555-1212",
                mPhoneAccountA0.getAccountHandle(), mConnectionServiceFixtureA,
                VideoProfile.STATE_TX_ENABLED, null);

        verifyAudioRoute(CallAudioState.ROUTE_SPEAKER);
    }

    /**
     * Tests audio routing for an outgoing video call made with transmit only video.  Expect to be
     * in speaker mode.  Note: The default UI does not support making one-way video calls, but the
     * APIs do and a third party incall UI could choose to support that.
     */
    @MediumTest
    public void testNoAutoSpeakerphoneOnOutgoing() throws Exception {
        // Start an incoming video call.
        IdPair ids = startAndMakeActiveOutgoingCall("650-555-1212",
                mPhoneAccountA0.getAccountHandle(), mConnectionServiceFixtureA,
                VideoProfile.STATE_AUDIO_ONLY, null);

        verifyAudioRoute(CallAudioState.ROUTE_EARPIECE);
    }

    /**
     * Tests to ensure an incoming audio-only call is routed to the earpiece.
     */
    @MediumTest
    public void testNoAutoSpeakerphoneOnIncoming() throws Exception {

        // Start an incoming video call.
        IdPair ids = startAndMakeActiveIncomingCall("650-555-1212",
                mPhoneAccountA0.getAccountHandle(), mConnectionServiceFixtureA,
                VideoProfile.STATE_AUDIO_ONLY);

        verifyAudioRoute(CallAudioState.ROUTE_EARPIECE);
    }

    /**
     * Verifies that the
     * {@link android.telecom.InCallService#onCallAudioStateChanged(CallAudioState)} change is
     * called with an expected route and number of changes.
     *
     * @param expectedRoute The expected audio route on the latest change.
     */
    private void verifyAudioRoute(int expectedRoute) throws Exception {
        // Capture all onCallAudioStateChanged callbacks to InCall.
        CallAudioRouteStateMachine carsm = mTelecomSystem.getCallsManager()
                .getCallAudioManager().getCallAudioRouteStateMachine();
        CallAudioModeStateMachine camsm = mTelecomSystem.getCallsManager()
                .getCallAudioManager().getCallAudioModeStateMachine();
        waitForHandlerAction(camsm.getHandler(), TEST_TIMEOUT);
        final boolean[] success = {true};
        carsm.sendMessage(CallAudioRouteStateMachine.RUN_RUNNABLE, (Runnable) () -> {
            ArgumentCaptor<CallAudioState> callAudioStateArgumentCaptor = ArgumentCaptor.forClass(
                    CallAudioState.class);
            try {
                verify(mInCallServiceFixtureX.getTestDouble(), atLeastOnce())
                        .onCallAudioStateChanged(callAudioStateArgumentCaptor.capture());
            } catch (RemoteException e) {
                fail("Remote exception in InCallServiceFixture");
            }
            List<CallAudioState> changes = callAudioStateArgumentCaptor.getAllValues();
            assertEquals(expectedRoute, changes.get(changes.size() - 1).getRoute());
            success[0] = true;
        });
        waitForHandlerAction(carsm.getHandler(), TEST_TIMEOUT);
        assertTrue(success[0]);
    }
}
