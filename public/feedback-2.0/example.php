<?php
session_start();
$data = json_decode($_SESSION['data']);

?>
<!DOCTYPE html>
<html>
<head>
    <script src="http://code.jquery.com/jquery-latest.min.js"></script>
	<script src="feedback.js"></script>
	<link rel="stylesheet" href="feedback.min.css" />
	<script type="text/javascript">
        document.addEventListener('DOMContentLoaded',
                                  function () {
        $.feedback({
            ajaxURL: 'example-listener.php',
            html2canvasURL: 'html2canvas.js',
            onClose: function() { window.location.reload(); }
        });
        }, false);
    </script>
</head>
<body>
    <header>
        <h1>Example Usage</h1>
    </header>
    <main>
        <div style="background: #9f0606; height: 300px; color: #fff;">
            <center>
                Hello there!
            </center>
        </div>
        <?php
            if (isset($data)) :
        ?>

        <img src="<?php echo $data->img; ?>"/>
        <?php
            endif;
        ?>
    </main>
</body>
</html>
