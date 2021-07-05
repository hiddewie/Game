const webpack = require('webpack')

module.exports = env => {
  const defines = {
    HOST: env.host,
  }
  console.info('Define plugin with defines', defines)
  const definePlugin = new webpack.DefinePlugin(defines)
  config.plugins.push(definePlugin)

  return config
}
