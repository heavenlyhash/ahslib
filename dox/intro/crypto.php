<?php require($_SERVER['DOCUMENT_ROOT']."/style/head.php"); ?>
<?php echo head("exultant.us AHSlib"); ?>
<?php echo theme(); ?>
<link rel='stylesheet' href='../style.css' type='text/css'>
<html id='ahslib-dox'><body class='std'>


<p>
	The <span class=package>ahs.crypto</span> packages attempt to provide a general set of interfaces that lets a designer specify what kind of properties (privacy, authentication, confidence, attribution, etc) a piece of binary data should have under what conditions, without directly immersing the designer in the exact details of every piece of key usage and what-to-mac-where-when sorts of things.
</p>

<div id=conventions>
	<style>
	#conventions 		{ border:1px solid; }
	#conventions .heading	{ margin:10px 30px 25px 80px; float:right; clear:both; text-decoration:underline; font-style:italic; letter-spacing:0.1ex; font-size:140%; }
	#conventions ul li	{ margin:15px; }
	#conventions ul li li	{ margin:05px; }
	#conventions code	{ font-size:120%; white-space:pre-wrap; }
	</style>
	<span class=heading>naming and labelling conventions</span>
	<ul>
	<li><code>&lang;x,y,z&rang;</code> is a tuple where each of the fields are distinguishable (regardless of whether it's an object in memory or an encoded form by length fields or delimiters).  <!-- (This usually also serves the role of representing concatenation.) -->
	<li><code>&lang;M&rang;</code> is a binary blob of whatever kind of message you want (probably something you can make into a meaningful message by calling something like <code>MyProtocolFace $theMessage = $codec.deserialize($M, MyProtocolFace.class);</code> ).
	<li>cryptographic keys are noted by their type:
		<ul>
		<li><code>Ks</code> denotes a symmetric key.
		<li><code>Kx</code> denotes a private key.
		<li><code>Ko</code> denotes a public key.
		<li><code>Kc</code> denotes something simply key-ish (i.e. binary, fixed-size, non-data)... like a nonce.
		</ul>
		A number or letter immediately following indicates a specific instance.
	<li>cryptographic functions have their key material specified in [brackets]:
		<ul>
		<li><code>Enc[Ks1](&lang;M&rang;)</code> denotes the symmetric encryption of M under the symmetric key Ks1.
		<li><code>Mac[Ks2](&lang;M&rang;)</code> denotes the symmetric MAC of M under the symmetric key Ks2.
		<li><code>Enp[Koa](&lang;M&rang;)</code> denotes the asymmetric encryption of M to the public key Koa.
		<li><code>Sig[Kxb](&lang;M&rang;)</code> denotes the asymmetric signiture of M by the private key Kxb.
		</ul>
	<li>
	</ul>
</div>

<br>

<div id=propertiesdefn>
	<table border=1>
	<tr><td><code>private</code>			</td>
		<td>A message is 'private' or ('confidential') if its content can only be transformed to cleartext by someone holding the key (either Ks or Kp, depending on if we're talking about Enc or Enp respectively).		</td></tr>
	<tr><td><code>nonmalleable</code>		</td>
		<td>A message is 'nonmalleable' (or 'authentic', or has 'integrity') if its content must have been written by someone holding the key (either Ks or Kp, depending on if we're talking about Mac or Sig respectively).		</td></tr>
	<tr><td><code>confident</code>			</td>
		<td>A message is 'confident' if it is possible to be sure that the message was decrypted correctly using the correct key (even assuming that the cipher used has no padding scheme and the cleartext is looks completely random).  This property is essential any time someone receiving a message might need to try multiple keys in order to choose the correct one, particularly if the message might contain other layers of encryption that we cannot open.		</td></tr>
	<tr><td><code>signed</code>			</td>
		<td>A message is 'signed' if is...well, signed... using a Kx.  (Either an extenal sig (readable by anyone) or an internal sig (only readable after decryption) counts.)
	<tr><td><code>attributable</code>		</td>
		<td>A message is 'attributable' if it is signed (and thus non-repudiable) and it is possible to tell who signed the message <i>without</i> being able to read the message (i.e. the sig is external/unencrypted). 		</td></tr>
	<tr><td><code>readily attributable</code>	</td>
			<td>'readily attributable' is the same as 'attributable', but the Ko used to sign is also attached, so you don't need to try potential signers; you just know after one check.		</td></tr>
		<!-- there is no option for an attached Ko without an external sig.  why would you ever do that? -->
	</table>
</div>

<br>

<table id=jeezycreezy>
<style>
#jeezycreezy		{ margin:15px; border:1px solid; margin:0; padding:0; border-spacing:0; }
#jeezycreezy td		{ border-top:1px solid; margin:0; padding:0; }
#jeezycreezy tr.head	{ height:100px; }
#jeezycreezy tr.head td	{ border-top:none; font-size:80%; font-family:serif; padding:0 5; }
#jeezycreezy .heading	{ margin:10px 30px 25px 80px; float:right; clear:both; text-decoration:underline; font-style:italic; letter-spacing:0.1ex; font-size:140%; }
#jeezycreezy code	{ font-family:monospace; font-size:120%; white-space:pre-wrap; }
#jeezycreezy td.alg	{ font-family:monospace; font-size:120%; white-space:pre-wrap; padding:0em 10px 1em 10px; }
#jeezycreezy td.cry	{ font-family:monospace; font-size:120%; white-space:pre-wrap; }
#jeezycreezy td.invok	{ font-family:monospace; font-size:120%; white-space:pre-wrap; }
#jeezycreezy .prop	{ width:22px; }
#jeezycreezy .prop div	{ width:16px; height:35px; margin-left:auto; margin-right:auto; }
#jeezycreezy .prop div.foo	{ position:relative; left:-45px; bottom:-0.2em; width:0px; }
#jeezycreezy .prop .yes		{ background-color:#0B0; }
#jeezycreezy .prop .no		{ background-color:#B00; }
#jeezycreezy .prop .todo	{ background-color:#F0F; }
#jeezycreezy .prop .irrel	{ background-color:#666; }
#jeezycreezy .rot270	{ writing-mode:tb-rl; -webkit-transform:rotate(-90deg); -moz-transform:rotate(-90deg); filter:progid:DXImageTransform.Microsoft.BasicImage(rotation=3); }
#jeezycreezy .rot250	{ writing-mode:tb-rl; -webkit-transform:rotate(-70deg); -moz-transform:rotate(-70deg); filter:progid:DXImageTransform.Microsoft.BasicImage(rotation=3); }
</style>
<!--

-->

<tr class="head"><td class="alg">
the sort of way it'd be coded
</td><td class="cry">
the way a math person would waste ink
<!--</td><td class="invok">
the way ahs is told to do it-->
</td>
<td class="prop rot270"><div class=foo>private			</div></td>
<td class="prop rot270"><div class=foo>nonmalleable		</div></td>
<td class="prop rot270"><div class=foo>confident		</div></td>
<td class="prop rot270"><div class=foo>signed			</div></td>
<td class="prop rot270"><div class=foo>attributable		</div></td>
<td class="prop rot270"><div class=foo>readily attributable	</div></td>
</tr>

<tr><td class=alg>
ret &lang;Enc[Ks](&lang;M&rang;)&rang;;
</td><td class=cry>
&lang;Enc[Ks](&lang;M&rang;)&rang;
<!--</td><td class="invok">
the way ahs is told to do it-->
</td>
<td class=prop><div class=yes></div>	</td>
<td class=prop><div class=no></div>	</td>
<td class=prop><div class=no></div>	</td>
<td class=prop><div class=irrel></div>	</td>
<td class=prop><div class=irrel></div>	</td>
<td class=prop><div class=irrel></div>	</td>
</tr>

<tr><td class=alg>
$e = Enc[Ks1](&lang;M&rang;);
$s = Mac[Ks2]($e);
ret &lang;$e,$s&rang;;
</td><td class=cry>
&lang;Enc[Ks1](&lang;M&rang;),Mac[Ks2](Enc[Ks1](&lang;M&rang;))&rang;
<!--</td><td class="invok">
the way ahs is told to do it-->
</td>
<td class=prop><div class=yes></div>	</td>
<td class=prop><div class=yes></div>	</td>
<td class=prop><div class=no></div>	</td>
<td class=prop><div class=irrel></div>	</td>
<td class=prop><div class=irrel></div>	</td>
<td class=prop><div class=irrel></div>	</td>
</tr>

<tr><td class=alg style="white-space:nowrap;"><br>
ret &lang;Enc[Ks](&lang;h(M),M&rang;)&rang;;
</td><td class=cry>
&lang;Enc[Ks](&lang;h(M),M&rang;)&rang;
<!--</td><td class="invok">
the way ahs is told to do it-->
</td>
<td class=prop><div class=yes></div>	</td>
<td class=prop><div class=no></div>	</td>
<td class=prop><div class=yes></div>	</td>
<td class=prop><div class=irrel></div>	</td>
<td class=prop><div class=irrel></div>	</td>
<td class=prop><div class=irrel></div>	</td>
</tr>

<tr><td class=alg>
$e = Enc[Ks1](&lang;h(M),M&rang;);
$s = Mac[Ks2]($e);
ret &lang;$e,$s&rang;;
</td><td class=cry>
&lang;Enc[Ks1](&lang;h(M),M&rang;),Mac[Ks2](Enc[Ks1](&lang;h(M),M&rang;))&rang;
<!--</td><td class="invok">
the way ahs is told to do it-->
</td>
<td class=prop><div class=yes></div>	</td>
<td class=prop><div class=yes></div>	</td>
<td class=prop><div class=yes></div>	</td>
<td class=prop><div class=irrel></div>	</td>
<td class=prop><div class=irrel></div>	</td>
<td class=prop><div class=irrel></div>	</td>
</tr>


</table>

// of course, this all kinda breaks down where it glazes over all of the details of what mode was used to transform block ciphers into stream and so on, and while in the back of my mind i've visualized almost sort of a series of Translators that translate one CryptoContainer to another while maintaining accuracy of their privacy level tags, I'm not sure that's really in any way practical, and the number of decorators required seems almost combinatorial until I figure out something smart.


</body></html>
<?php echo foot(); ?>