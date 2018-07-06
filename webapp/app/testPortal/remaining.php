<?php

/**
 * Provides access to remaining food in JSON and in UI
 *
 * Request user as GET param.
 */

include_once "libs/foodGenerator.php";
include_once "libs/foodOrderTools.php";

$menuEntries = array();

// Generates list of food Ids (The food is generated from food Ids)
// Actual date rounded to days
$time = ((int)(time() / (60 * 60 * 24))) * (60 * 60 * 24);
// Remaining counts are displayed only for current day
$dayIndex = 0;
for ($groupIndex = 1; $groupIndex <= 3; $groupIndex++) {
    // Each day has 3 types of food
    for ($groupVariantIndex = 1; $groupVariantIndex <= 3; $groupVariantIndex++) {
        // Each food has 3 variants of food
        $foodId = "$time$groupIndex$groupVariantIndex";
        $menuEntry = generateFood($foodId, 1);
        if ($menuEntry->features->remainingFood || $menuEntry->features->foodStock) {
            // Only some foods supports remaining displaying
            if ($menuEntry->features->remainingFood) {
                // Remaining now is generated from time
                $menuEntry->remainingToTake = intval((60 - (time() % 60)) / 5);
            }
            if ($menuEntry->features->foodStock) {
                // Remaining now is generated from foodStock
                $menuEntry->remainingToOrder = remainingInFoodStock($foodId, -1);
            }
            $menuEntries[] = $menuEntry;
        }
    }
}
if ($_GET["format"] == "text") {
    ?>
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="UTF-8">
        <title>
            sfm - testPortal - Remaining
        </title>
        <link rel="stylesheet" type="text/css" href="res/style.css"/>
    </head>
    <body>
    <h1>
        Remaining
    </h1>
    <table>
        <tr>
            <th> Date</th>
            <th> Name of food</th>
            <th> Remaining to take</th>
            <th> Remaining to order</th>
        </tr>
        <?php
        foreach ($menuEntries as $el) {
            echo "<tr>";
            echo "<td> " . date("Y-m-d ", $el->date / 1000) . " </td>";
            echo "<td> $el->foodName </td>";
            echo "<td> $el->remainingToTake </td>";
            echo "<td> $el->remainingToOrder </td>";
            echo "</tr>";
        }
        ?>
    </table>
    </body>
    </html>
    <?php
} else {
    $data = array();

    // Binds rows from menu to array that can be printed in JSON
    foreach ($menuEntries as $menuEntry) {
        // Rename columns to corresponding names in JSON
        $row_array['relativeId'] = $menuEntry->internalID;
        $row_array['text'] = $menuEntry->foodText;
        $row_array['group'] = $menuEntry->foodGroup;
        $row_array['label'] = $menuEntry->foodName;
        $row_array['date'] = $menuEntry->date;
        // $row_array['remainingToTake'] = ... not used needed
        // $row_array['remainingToOrder'] = ... not used needed
        // $row_array['extra'] = ... not needed
        $row_array['price'] = $menuEntry->price;

        $features = 0;
        if ($menuEntry->features->orders) {
            $features |= 3;
        }
        if ($menuEntry->features->foodStock) {
            $features |= 4;
        }
        $row_array['features'] = $features;
        $row_array['remainingToOrder'] = $menuEntry->remainingToOrder;
        $row_array['remainingToTake'] = $menuEntry->remainingToTake;

        array_push($data, $row_array);
    }

    // Prints JSON (in standard case without UI)
    echo json_encode($data);
    echo "\n\n\n<endora>";
}
?>
