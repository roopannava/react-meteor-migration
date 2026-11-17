import { createRoot } from "react-dom/client";
import { Meteor } from "meteor/meteor";
import { Provider } from "react-redux";
import { store } from "/imports/store";
import { App } from "/imports/ui/App";
import "/imports/ui/styles.css";

Meteor.startup(() => {
  const container = document.getElementById("react-target");
  const root = createRoot(container);
  root.render(
      <Provider store={store}>
        <App />
      </Provider>
  );
});