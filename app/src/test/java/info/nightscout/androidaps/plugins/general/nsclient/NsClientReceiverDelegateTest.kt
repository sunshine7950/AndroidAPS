package info.nightscout.androidaps.plugins.general.nsclient

import android.content.Context
import info.nightscout.androidaps.MainApp
import info.nightscout.androidaps.R
import info.nightscout.androidaps.TestBase
import info.nightscout.androidaps.events.EventChargingState
import info.nightscout.androidaps.events.EventNetworkChange
import info.nightscout.androidaps.logging.AAPSLogger
import info.nightscout.androidaps.plugins.bus.RxBusWrapper
import info.nightscout.androidaps.receivers.ReceiverStatusStore
import info.nightscout.androidaps.utils.resources.ResourceHelper
import info.nightscout.androidaps.utils.sharedPreferences.SP
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

@RunWith(PowerMockRunner::class)
@PrepareForTest(MainApp::class, SP::class, Context::class)
class NsClientReceiverDelegateTest : TestBase() {

    @Mock lateinit var aapsLogger: AAPSLogger
    @Mock lateinit var context: Context
    @Mock lateinit var sp: SP
    @Mock lateinit var resourceHelper: ResourceHelper

    lateinit var receiverStatusStore : ReceiverStatusStore
    val rxBus: RxBusWrapper = RxBusWrapper()

    private var sut: NsClientReceiverDelegate? = null

    @Before fun prepare() {
        receiverStatusStore = ReceiverStatusStore(context)
        System.setProperty("disableFirebase", "true")
        PowerMockito.mockStatic(MainApp::class.java)
        val mainApp: MainApp = mock(MainApp::class.java)
        `when`(MainApp.instance()).thenReturn(mainApp)
        PowerMockito.mockStatic(SP::class.java)
        `when`(sp.getLong(anyInt(), anyLong())).thenReturn(0L)
        `when`(sp.getBoolean(anyInt(), anyBoolean())).thenReturn(false)
        `when`(sp.getInt(anyInt(), anyInt())).thenReturn(0)
        `when`(sp.getString(anyInt(), anyString())).thenReturn("")

        sut = NsClientReceiverDelegate(aapsLogger, context, rxBus, resourceHelper, sp, receiverStatusStore)
    }

    @Test fun testCalculateStatusChargingState() {
        PowerMockito.mockStatic(SP::class.java)
        Mockito.`when`(sp.getBoolean(ArgumentMatchers.anyInt(), ArgumentMatchers.anyBoolean())).thenReturn(false)
        var ev = EventChargingState(true)
        Assert.assertTrue(sut!!.calculateStatus(ev))
        ev = EventChargingState(false)
        Assert.assertTrue(sut!!.calculateStatus(ev))
        Mockito.`when`(sp.getBoolean(ArgumentMatchers.anyInt(), ArgumentMatchers.anyBoolean())).thenReturn(true)
        ev = EventChargingState(true)
        Assert.assertTrue(sut!!.calculateStatus(ev))
        ev = EventChargingState(false)
        Assert.assertTrue(!sut!!.calculateStatus(ev))
    }

    @Test fun testCalculateStatusNetworkState() {
        PowerMockito.mockStatic(SP::class.java)
        // wifiOnly = false
        // allowRoaming = false as well
        Mockito.`when`(sp.getBoolean(ArgumentMatchers.anyInt(), ArgumentMatchers.anyBoolean())).thenReturn(false)
        Mockito.`when`(sp.getString(ArgumentMatchers.anyInt(), ArgumentMatchers.anyString())).thenReturn("")
        val ev = EventNetworkChange()
        ev.ssid = "<unknown ssid>"
        ev.mobileConnected = true
        ev.wifiConnected = true
        Assert.assertTrue(sut!!.calculateStatus(ev))
        ev.ssid = "test"
        Mockito.`when`(sp.getString(ArgumentMatchers.anyInt(), ArgumentMatchers.anyString())).thenReturn("\"test\"")
        Assert.assertTrue(sut!!.calculateStatus(ev))
        ev.ssid = "\"test\""
        Assert.assertTrue(sut!!.calculateStatus(ev))
        ev.wifiConnected = false
        Assert.assertTrue(sut!!.calculateStatus(ev))

        // wifiOnly = true
        // allowRoaming = true as well
        Mockito.`when`(sp.getBoolean(ArgumentMatchers.anyInt(), ArgumentMatchers.anyBoolean())).thenReturn(true)
        ev.wifiConnected = true
        Assert.assertTrue(sut!!.calculateStatus(ev))
        ev.wifiConnected = false
        Assert.assertTrue(!sut!!.calculateStatus(ev))

        // wifiOnly = false
        // allowRoaming = false as well
        Mockito.`when`(sp.getBoolean(ArgumentMatchers.anyInt(), ArgumentMatchers.anyBoolean())).thenReturn(false)
        ev.wifiConnected = false
        ev.roaming = true
        Assert.assertTrue(!sut!!.calculateStatus(ev))

        // wifiOnly = false
        // allowRoaming = true
        Mockito.`when`(sp.getBoolean(R.string.key_ns_wifionly, false)).thenReturn(false)
        Mockito.`when`(sp.getBoolean(R.string.key_ns_allowroaming, true)).thenReturn(true)
        ev.wifiConnected = false
        ev.roaming = true
        Assert.assertTrue(sut!!.calculateStatus(ev))

        // wifiOnly = true
        // allowRoaming = true
        Mockito.`when`(sp.getBoolean(R.string.key_ns_wifionly, false)).thenReturn(true)
        Mockito.`when`(sp.getBoolean(R.string.key_ns_allowroaming, true)).thenReturn(true)
        ev.wifiConnected = false
        ev.roaming = true
        Assert.assertTrue(!sut!!.calculateStatus(ev))

        // wifiOnly = true
        // allowRoaming = true
        Mockito.`when`(sp.getBoolean(R.string.key_ns_wifionly, false)).thenReturn(true)
        Mockito.`when`(sp.getBoolean(R.string.key_ns_allowroaming, true)).thenReturn(true)
        ev.wifiConnected = true
        ev.roaming = true
        Assert.assertTrue(sut!!.calculateStatus(ev))

        // wifiOnly = false
        // allowRoaming = false
        Mockito.`when`(sp.getBoolean(R.string.key_ns_wifionly, false)).thenReturn(false)
        Mockito.`when`(sp.getBoolean(R.string.key_ns_allowroaming, true)).thenReturn(false)
        ev.wifiConnected = true
        ev.roaming = true
        Assert.assertTrue(sut!!.calculateStatus(ev))
    }
}