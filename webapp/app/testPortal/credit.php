<?php

/**
 * Provides credit info in JSON and in UI
 *
 * Request user as GET param.
 */

/**
 * Class UserCredit is used for putting results into JSON
 */
class UserCredit
{
    public $credit;
}

// Generates credit info based on user id
$userData = new UserCredit();
$userData->credit = 512 + 10 * $_GET["user"];

if ($_GET["format"] == "text") {
    ?>
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="UTF-8">
        <title>
            sfm - testPortal - Credit
        </title>
        <link rel="stylesheet" type="text/css" href="res/style.css"/>
    </head>
    <body>
    <h1>
        Credit
    </h1>
    <?php include "libs/navBar.php" ?>
    <table>
        <tr>
            <th> Credit</th>
        </tr>
        <tr>
            <td> <?php echo $userData->credit; ?> </td>
        </tr>
    </table>
    </body>
    </html>
    <?php
} else {
    // Prints JSON (in standard case without UI)
    echo json_encode($userData);
    echo "\n\n\n<endora>";
}
?>
