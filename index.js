import { NativeModules, NativeEventEmitter } from "react-native";

const { ShareMenu } = NativeModules;

const EventEmitter = new NativeEventEmitter(ShareMenu);

const NEW_SHARE_EVENT_NAME = "NewShareEvent";

export const ShareMenuReactView = {
  dismissExtension(error = null) {
    ShareMenu.dismissExtension(error);
  },
  openApp() {
    ShareMenu.openApp();
  },
  continueInApp(extraData = null) {
    ShareMenu.continueInApp(extraData);
  },
  data() {
    return ShareMenu.data();
  },
};

export default {
  /**
   * @deprecated Use `getInitialShare` instead. This is here for backwards compatibility.
   */
  getSharedText(callback) {
    this.getInitialShare(callback);
  },
  getInitialShare(callback) {
    ShareMenu.getSharedText(callback);
  },
  addNewShareListener(callback) {
    const subscription = EventEmitter.addListener(
      NEW_SHARE_EVENT_NAME,
      callback
    );

    return subscription;
  },
  data: () => ShareMenu.data(),
  openURL: (url) => ShareMenu.openURL(url),
};
