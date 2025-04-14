function createLoadingOverlay() {
    if (document.getElementById("loading_spinner") !== null) {
        // spinner already exist. Log and exit
        console.log("Loading spinner already exist. Skipping");
        return;
    }

    let container = document.createElement("div");
    container.className = "loading_overlay";
    container.id = "loading_overlay";

    let image = document.createElement("img");
    image.id = "loading_spinner";
    image.className = "loading_spinner";
    image.src = "/loading.gif";
    image.alt = "Loading...";

    container.appendChild(image);

    document.getElementsByTagName("body")[0].appendChild(container);
}

function removeLoadingOverlay() {
    let container = document.getElementById("loading_overlay");
    let image = document.getElementById("loading_spinner");

    if (image !== null) {
        image.remove();
    }
    if (container !== null) {
        container.remove();
    }
}