<?php
/**
 * UI for specifying new history entry
 *
 * Request user as GET parameter.
 */
?>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>
        sfm - testPortal - New payment entry
    </title>
    <link rel="stylesheet" type="text/css" href="../res/style.css"/>
</head>
<body>
<h1>
    New payment entry
</h1>
<form action="addPayment.php" method="get">
    <input type="hidden" name="user" value="<?php echo $_GET["user"]; ?>"/>
    <table>
        <tr>
            <th> When</th>
            <th> Description</th>
            <th> Price</th>
            <th> Quantity</th>
            <th> Actions</th>
        </tr>
        <tr>
            <td><input type="date" name="date" min="0" step="1" value="1" required/></td>
            <td><input type="text" name="description" required></td>
            <td><input type="number" name="price" min="0" step="1" required/></td>
            <td><input type="number" name="quantity" min="0" step="1" value="1" required/></td>
            <td><input type="submit"/></td>
        </tr>
    </table>
</form>
</body>
</html>
