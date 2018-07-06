<?php
/**
 * UI for specifying new order or putting food to food stock
 *
 * Request user as GET parameter.
 */

include_once "../libs/foodGenerator.php";
include_once "../libs/foodOrderTools.php";
?>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>
        sfm - testPortal - Action detail
    </title>
    <link rel="stylesheet" type="text/css" href="../res/style.css"/>
</head>
<body>
<?php
$foodEntry = generateFood($_GET["id"], $_GET["user"]);
$inWholeStock = remainingInFoodStock($_GET["id"]);

$conn = connectDbFoodOrder();

// Check connection
if ($conn->connect_error) {
    die("Connection failed: " . $conn->connect_error);
}

$stmt = $conn->prepare("SELECT * FROM FoodOrder WHERE FOFoodID=? AND FOUserID=? AND FOType='standard'");
$stmt->bind_param('ii', $_GET["id"], $_GET["user"]);

// Check results
if (!$stmt->execute()) {
    die("Query failed: " . $conn->error);
}

$result = $stmt->get_result();
$row = $result->fetch_assoc();

$conn->close();

$reserved = intval($row["FOReserved"]);
$offered = intval($row["FOOffered"]);
$taken = intval($row["FOTaken"]);
?>
<h1>
    Change detail
</h1>
<form action="../order.php" method="get">
    <input type="hidden" name="id" value="<?php echo $_GET["id"]; ?>"/>
    <input type="hidden" name="user" value="<?php echo $_GET["user"]; ?>"/>
    <input type="hidden" name="dev" value="<?php echo $_GET["dev"]; ?>"/>
    <input type="hidden" name="format" value="text"/>
    <table>
        <tr>
            <th> Food name</th>
            <th> Date</th>
            <th> Reserved</th>
            <th> Offered</th>
            <th> Taken</th>
            <th> Actions</th>
        </tr>
        <tr>
            <td><?php echo $foodEntry->foodName; ?></td>
            <td><?php echo date("Y-m-d ", $foodEntry->date / 1000); ?></td>
            <td>
                <input type="number" name="reserved" <?php
                if ($foodEntry->features->orders || $_GET["dev"] == "true") {
                    echo "min=\"0\"";
                } else {
                    echo "min=\"$reserved\"";
                }
                ?> step="1" <?php
                if (!$foodEntry->features->orders && $_GET["dev"] != "true") {
                    echo "max=\"" . ($reserved + $inWholeStock - $offered) . "\"";
                }
                ?> value="<? echo $reserved; ?>"/>
            </td>
            <td>
                <input type="number" name="offered" min="0" step="1" <?php
                if ($foodEntry->features->orders && $_GET["dev"] != "true") {
                    echo "max=\"0\"";
                } else {
                    echo "max=\"$reserved\"";
                } ?> value="<? echo $offered; ?>"/>
            </td>
            <td>
                <input type="number" name="taken" min="0" step="1" <?
                echo "max=\"$reserved\""
                ?> value="<? echo $taken; ?>"/>
            </td>
            <td>
                <input type="submit"/>
            </td>
        </tr>
    </table>
</form>
</body>
</html>
