module.exports = function (config) {
  config.set({
    browsers: ['ChromeHeadless'],
    basePath: 'karma',
    files: ['ci.js'],
    frameworks: ['cljs-test'],
    plugins: [
        'karma-cljs-test',
        'karma-chrome-launcher',
        'karma-junit-reporter'
    ],
    colors: true,
    logLevel: config.LOG_INFO,
    client: {
      args: ['shadow.test.karma.init'],
      singleRun: true
    },

    reporters: ['progress', 'junit'],

    // the default configuration
    junitReporter: {
      outputDir: 'results', // results will be saved as outputDir/browserName.xml
      outputFile: undefined, // if included, results will be saved as outputDir/browserName/outputFile
      suite: '' // suite will become the package name attribute in xml testsuite element
    }
  })
}
