import { Meteor } from "meteor/meteor";
import { LinksCollection } from "/imports/api/links";
import { Random } from "meteor/random";
import { StocksCollection } from "/imports/api/stocks";


const BACKEND_URL = "http://localhost:8080/api/stocks/top-winners";

// Pull GetTenTopWinnersOnNyse from the Spring Boot backend and store each row
// in the stocks collection. Upsert by symbol so repeated refreshes update rows
// in place instead of creating duplicates.
async function loadTopWinners() {
  try {
    const response = await fetch(BACKEND_URL);
    if (!response.ok) {
      console.error(`[stocks] backend returned HTTP ${response.status}`);
      return;
    }
    const rows = await response.json(); // [{ symbol, name, price, changePercent }]
    for (const row of rows) {
      await StocksCollection.upsertAsync({ symbol: row.symbol }, { $set: row });
    }
    console.log(`[stocks] loaded ${rows.length} top winners from backend`);
  } catch (err) {
    console.error("[stocks] failed to load from backend:", err.message);
  }
}




async function insertLink({ title, url }) {
  await LinksCollection.insertAsync({ title, url, createdAt: new Date() });
}

Meteor.startup(async () => {
  await loadTopWinners();
  // If the Links collection is empty, add some data.
  if ((await LinksCollection.find().countAsync()) === 0) {
    await insertLink({
      title: "Do the Tutorial",
      url: "https://docs.meteor.com/tutorials/react/",
    });

    await insertLink({
      title: "Follow the Guide",
      url: "https://docs.meteor.com/tutorials/application-structure/",
    });

    await insertLink({
      title: "Read the Docs",
      url: "https://docs.meteor.com",
    });

    await insertLink({
      title: "Discussions",
      url: "https://forums.meteor.com",
    });

    await insertLink({
      title: "Join us on Discord",
      url: "https://discord.gg/6mS3wHNg",
    });

    await insertLink({
      title: "Deploying in Galaxy",
      url: "https://www.meteor.com/hosting",
    });
  }

  // We publish the entire Links collection to all clients.
  // In order to be fetched in real-time to the clients
  Meteor.publish("links", function () {
    return LinksCollection.find();
  });
  // Publish the stocks collection so subscribed clients receive the rows
  // reactively — when the server updates a row, every subscriber updates too.
  Meteor.publish("topWinners", function () {
    return StocksCollection.find({}, { sort: { changePercent: -1 } });
  });
});

Meteor.methods({
  about() {
    return `This is a Meteor application running React with React Router. this is a generated id: ${Random.id()}`;
  },
});
