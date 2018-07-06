<?php

/**
 * Provides connection between Android app ond web server with data about portals
 */
 
include_once "sfmAjaxReply.php";

include_once "sfmDbConnection.php";

// Check connection
if ($conn->connect_error) {
    die("Connection failed: " . $conn->connect_error);
}

$portals = array();

// Ask the query
$sql = "SELECT * FROM Portal";
$result = $conn->query($sql);

// Binds rows from db to array that can be printed in JSON
while ($row = $result->fetch_assoc()) {
    // Rename columns to corresponding names in JSON
    $row_array['ID'] = intval($row['PID']);
    $row_array['PGID'] = intval($row['PGID']);
    $row_array['Name'] = $row['PName'];
    $row_array['Plugin'] = $row['PPlugin'];
    $row_array['Ref'] = $row['PRef'];
    $row_array['LocN'] = floatval($row['PLocN']);
    $row_array['LocE'] = floatval($row['PLocE']);
    $row_array['Extra'] = $row['PExtra'];
    $row_array['Security'] = intval($row['PSecurity']);
    $row_array['Features'] = intval($row['PFeatures']);

    array_push($portals, $row_array);
}

$conn->close();

// Provides read-only user interface with data (if needed)
if ($_GET["format"] == "text") {
    ?>
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="UTF-8">
        <title>
            sfm - Food portals
        </title>
        <style>
            table {
                border-collapse: collapse;
                width: 100%;
            }

            th, td {
                text-align: left;
                padding: 8px;
            }

            tr:nth-child(even) {
                background-color: #ffccbc
            }

            th {
                background-color: #f44336;
                color: white;
            }
        </style>
    </head>
    <body>
    <h1>
        Food portals
    </h1>
    <table>
        <tr>
            <th> ID</th>
            <th> Name</th>
            <th> Reference</th>
            <th> Location</th>
        </tr>
        <?php
        foreach ($portals as $portal) {
            echo "<tr>";
            echo "<td>" . $portal['ID'] . "</td>";
            echo "<td>" . $portal['Name'] . "</td>";
            echo "<td>" . $portal['Ref'] . "</td>";
            echo "<td> <a href=\"http://maps.google.com/maps?q=" . $portal['LocN'] . "," . $portal['LocE'] . "\"> " . $portal['LocN'] . "N, " . $portal['LocE'] . "E </a> </td>";
            echo "</tr>";
        }
        ?>
    </table>
    </body>
    </html>
    <?php
    //End of user interface part
} else {
    $response = new SfmAjaxReply();
    $response->replyType = "portals";
    $response->data = $portals;
    // Prints JSON (in standard case without UI)
    echo json_encode($response);
    echo "\n\n\n<endora>";
}
?>
