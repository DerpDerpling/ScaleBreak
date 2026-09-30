# ScaleBreak

ScaleBreak is a client side Fabric mod that makes block breaking feel more immersive by smoothly shrinking blocks while they are being mined.

## Compatibility

ScaleBreak currently has compatibility with Sodium, Iris, and Better Block Entities, and has been tested with the Fabulously Optimized modpack.

If you encounter any issues with mods that prevent ScaleBreak from working, please leave an [issue on GitHub](https://github.com/DerpDerpling/ScaleBreak/issues) and include the mods you are using.

## Configuration

ScaleBreak can be configured in-game using:

```
/scalebreak
```

Available configuration commands include:

```json
/scalebreak toggle
/scalebreak shrinkAmount <value> //how small the block gets before breaking
/scalebreak shrinkSpeed <value> // how fast the shrinking animation goes to the next state
/scalebreak recoverySpeed <value> //how fast the block returns to full size
/scalebreak multiblocks <true|false> //like beds and doors breaking together
/scalebreak reload
/scalebreak reset
```