/*
 * Copyright (C) 2006 The Android Open Source Project
 * Copyright (c) 2011-12 Code Aurora Forum. All rights reserved.
 * Not a Contribution, Apache license notifications and license are retained
 * for attribution purposes only.
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
 * limitations under the License.
 */

package com.android.internal.telephony.msim;

import android.net.LinkCapabilities;
import android.net.LinkProperties;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.CellInfo;
import android.telephony.MSimTelephonyManager;
import android.telephony.ServiceState;
import android.util.Log;

import com.android.internal.telephony.Call;
import com.android.internal.telephony.DefaultPhoneNotifier;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.ITelephonyRegistryMSim;

/**
 * broadcast intents
 */
public class MSimDefaultPhoneNotifier extends DefaultPhoneNotifier {
	static final String LOG_TAG = "GSM";
	private ITelephonyRegistryMSim mMSimRegistry = ITelephonyRegistryMSim.Stub
			.asInterface(ServiceManager.getService("telephony.msim.registry"));

	@Override
	public void notifyPhoneState(Phone sender) {
		Call ringingCall = sender.getRingingCall();
		int subscription = sender.getSubscription();
		String incomingNumber = "";
		if (ringingCall != null && ringingCall.getEarliestConnection() != null) {
			incomingNumber = ringingCall.getEarliestConnection().getAddress();
		}
		try {
			mMSimRegistry.notifyCallState(convertCallState(sender.getState()),
					incomingNumber, subscription);
		} catch (RemoteException ex) {
			// system process is dead
			ex.printStackTrace();
		}
	}

	@Override
	public void notifyServiceState(Phone sender) {
		ServiceState ss = sender.getServiceState();
		int subscription = sender.getSubscription();
		if (ss == null) {
			ss = new ServiceState();
			ss.setStateOutOfService();
		}
		try {
			mMSimRegistry.notifyServiceState(ss, subscription);
		} catch (RemoteException ex) {
			// system process is dead
			ex.printStackTrace();
		}
	}

	@Override
	public void notifySignalStrength(Phone sender) {
		int subscription = sender.getSubscription();
		try {
			mMSimRegistry.notifySignalStrength(sender.getSignalStrength(),
					subscription);
		} catch (RemoteException ex) {
			// system process is dead
			ex.printStackTrace();
		}
	}

	@Override
	public void notifyMessageWaitingChanged(Phone sender) {
		int subscription = sender.getSubscription();
		try {
			mMSimRegistry.notifyMessageWaitingChanged(
					sender.getMessageWaitingIndicator(), subscription);
		} catch (RemoteException ex) {
			// system process is dead
			ex.printStackTrace();
		}
	}

	@Override
	public void notifyCallForwardingChanged(Phone sender) {
		int subscription = sender.getSubscription();
		try {
			mMSimRegistry.notifyCallForwardingChanged(
					sender.getCallForwardingIndicator(), subscription);
		} catch (RemoteException ex) {
			// system process is dead
			ex.printStackTrace();
		}
	}

	@Override
	public void notifyDataActivity(Phone sender) {
		try {
			mMSimRegistry.notifyDataActivity(convertDataActivityState(sender
					.getDataActivityState()));
		} catch (RemoteException ex) {
			// system process is dead
			ex.printStackTrace();
		}
	}

	@Override
	public void notifyDataConnection(Phone sender, String reason,
			String apnType, Phone.DataState state) {
		doNotifyDataConnection(sender, reason, apnType, state);
	}

	protected void doNotifyDataConnection(Phone sender, String reason,
			String apnType, Phone.DataState state) {

		int subscription = sender.getSubscription();
		int dds = MSimPhoneFactory.getDataSubscription();
		log("subscription = " + subscription + ", DDS = " + dds);
		if (subscription != dds) {
			// This is not the current DDS, do not notify data connection state
			return;
		}

		// TODO
		// use apnType as the key to which connection we're talking about.
		// pass apnType back up to fetch particular for this one.
		// MSimTelephonyManager telephony = MSimTelephonyManager.getDefault();
		LinkProperties linkProperties = null;
		LinkCapabilities linkCapabilities = null;
		boolean roaming = false;

		if (state == Phone.DataState.CONNECTED) {
			linkProperties = sender.getLinkProperties(apnType);
			linkCapabilities = sender.getLinkCapabilities(apnType);
		}
		ServiceState ss = sender.getServiceState();
		if (ss != null)
			roaming = ss.getRoaming();

		try {
			mMSimRegistry.notifyDataConnection(convertDataState(state),
					sender.isDataConnectivityPossible(apnType), reason,
					sender.getActiveApnHost(apnType), apnType, linkProperties,
					linkCapabilities,
					MSimTelephonyManager.getNetworkType(subscription), roaming);
		} catch (RemoteException ex) {
			// system process is dead
			ex.printStackTrace();
		}
	}

	@Override
	public void notifyDataConnectionFailed(Phone sender, String reason,
			String apnType) {
		try {
			mMSimRegistry.notifyDataConnectionFailed(reason, apnType);
		} catch (RemoteException ex) {
			// system process is dead
			ex.printStackTrace();
		}
	}

	@Override
	public void notifyCellLocation(Phone sender) {
		int subscription = sender.getSubscription();
		Bundle data = new Bundle();
		sender.getCellLocation().fillInNotifierBundle(data);
		try {
			mMSimRegistry.notifyCellLocation(data, subscription);
		} catch (RemoteException ex) {
			// system process is dead
			ex.printStackTrace();
		}
	}

	@Override
	public void notifyCellInfo(Phone sender, CellInfo cellInfo) {
		int subscription = sender.getSubscription();
		try {
			mMSimRegistry.notifyCellInfo(cellInfo, subscription);
		} catch (RemoteException ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public void notifyOtaspChanged(Phone sender, int otaspMode) {
		try {
			mMSimRegistry.notifyOtaspChanged(otaspMode);
		} catch (RemoteException ex) {
			// system process is dead
			ex.printStackTrace();
		}
	}

	private void log(String s) {
		Log.d(LOG_TAG, "[MSimDefaultPhoneNotifier] " + s);
	}
}
