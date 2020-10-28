/*
 * Author:  Martin Novak <mn@puakma.net>
 * Project: Puakma Vortex
 * Date:    20/09/2006
 * 
 * Copyright (c) 2006 webWise Network Consultants Pty Ltd, Australia,
 * http://www.wnc.net.au, all rights reserved
 *
 * Publishing, providing further or using of this program is prohibited
 * without previous written permission of author. Publishing or providing further
 * of the contents of this file is prohibited without prevous written permission
 * of the author.
 */
package puakma.vortex.editors.dbschema.generator;

//import org.eclipse.datatools.sqltools.sqleditor.SQLEditor;
import org.eclipse.datatools.sqltools.sqleditor.internal.PreferenceConstants;
import org.eclipse.datatools.sqltools.sqleditor.internal.SQLEditorPlugin;
import org.eclipse.datatools.sqltools.sqleditor.internal.indent.SQLAutoIndentStrategy;
import org.eclipse.datatools.sqltools.sqleditor.internal.indent.SQLCommentAutoIndentStrategy;
import org.eclipse.datatools.sqltools.sqleditor.internal.indent.SQLStringAutoIndentStrategy;
//import org.eclipse.datatools.sqltools.sqleditor.internal.sql.BestMatchHover;
//import org.eclipse.datatools.sqltools.sqleditor.internal.sql.ISQLDBProposalsService;
import org.eclipse.datatools.sqltools.editor.contentassist.ISQLDBProposalsService;
import org.eclipse.datatools.sqltools.sqleditor.internal.sql.SQLCodeScanner;
import org.eclipse.datatools.sqltools.sqleditor.internal.sql.SQLCompletionProcessor;
import org.eclipse.datatools.sqltools.sqleditor.internal.sql.SQLDoubleClickStrategy;
import org.eclipse.datatools.sqltools.sqleditor.internal.sql.SQLPartitionScanner;
import org.eclipse.datatools.sqltools.sqleditor.internal.utils.SQLColorProvider;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.formatter.ContentFormatter;
import org.eclipse.jface.text.formatter.IContentFormatter;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.BufferedRuleBasedScanner;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.graphics.RGB;

/**
 * This class defines the editor add-ons; content assist, content formatter, highlighting,
 * auto-indent strategy, double click strategy.
 */
public class SQLSimpleSourceViewerConfiguration extends SourceViewerConfiguration
{
  /** The editor with which this configuration is associated. */
  //private SQLEditor fEditor;

  /** The completion processor for completing the user's typing. */
  private SQLCompletionProcessor fCompletionProcessor;

  /** The content assist proposal service for database objects. */
  private ISQLDBProposalsService fDBProposalsService;

  /**
   * This class implements a single token scanner.
   */
  static class SingleTokenScanner extends BufferedRuleBasedScanner
  {
    public SingleTokenScanner(TextAttribute attribute)
    {
      setDefaultReturnToken(new Token(attribute));
    }
  }

  /**
   * Constructs an instance of this class with the given SQLEditor to configure.
   * 
   * @param editor the SQLEditor to configure
   */
  public SQLSimpleSourceViewerConfiguration() //SQLEditor editor)
  {
    //fEditor = editor;
    fCompletionProcessor = new SQLCompletionProcessor();
  }

  /**
   * Returns the annotation hover which will provide the information to be shown in a
   * hover popup window when requested for the given source viewer.
   * 
   * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getAnnotationHover(org.eclipse.jface.text.source.ISourceViewer)
   */
  public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer)
  {
    return super.getAnnotationHover(sourceViewer);
    //return new SQLAnnotationHover(getSQLEditor());
  }

  /*
   * @see SourceViewerConfiguration#getAutoIndentStrategy(ISourceViewer, String)
   */
  public IAutoEditStrategy[] getAutoEditStrategies(ISourceViewer sourceViewer, String contentType)
  {
    if(SQLPartitionScanner.SQL_CODE.equals(contentType)
       || IDocument.DEFAULT_CONTENT_TYPE.equals(contentType)) {
      return new IAutoEditStrategy[] { 
          new SQLAutoIndentStrategy(SQLPartitionScanner.SQL_PARTITIONING) 
          };
    }
    else if(SQLPartitionScanner.SQL_COMMENT.equals(contentType)
            || SQLPartitionScanner.SQL_MULTILINE_COMMENT.equals(contentType)) {
      return new IAutoEditStrategy[] { 
          new SQLCommentAutoIndentStrategy(SQLPartitionScanner.SQL_PARTITIONING) 
          };
    }
    else if(SQLPartitionScanner.SQL_STRING.equals(contentType)
            || SQLPartitionScanner.SQL_DOUBLE_QUOTES_IDENTIFIER.equals(contentType)) {
      return new IAutoEditStrategy[] { 
          new SQLStringAutoIndentStrategy(SQLPartitionScanner.SQL_STRING)
          };
    }
    return null;
  }

  /**
   * Returns the configured partitioning for the given source viewer. The partitioning is
   * used when the querying content types from the source viewer's input document.
   * 
   * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getConfiguredDocumentPartitioning(org.eclipse.jface.text.source.ISourceViewer)
   */
  public String getConfiguredDocumentPartitioning(ISourceViewer sourceViewer)
  {
    return SQLPartitionScanner.SQL_PARTITIONING;
  }

  /**
   * Creates, initializes, and returns the ContentAssistant to use with this editor.
   * 
   * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getContentAssistant(ISourceViewer)
   */
  public IContentAssistant getContentAssistant(ISourceViewer sourceViewer)
  {
    IPreferenceStore store = SQLEditorPlugin.getDefault().getPreferenceStore();
    ContentAssistant assistant = new ContentAssistant();

    assistant.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));

    // Set content assist processors for various content types.
    if(fCompletionProcessor == null) {
      fCompletionProcessor = new SQLCompletionProcessor();
    }
    assistant.setContentAssistProcessor(fCompletionProcessor, IDocument.DEFAULT_CONTENT_TYPE);
    ISQLDBProposalsService proposalsService = getDBProposalsService();
    if(proposalsService != null) {
      fCompletionProcessor.setDBProposalsService(proposalsService);
    }

    // Configure how content assist information will appear.
    assistant.enableAutoActivation(store.getBoolean(PreferenceConstants.ENABLE_AUTO_ACTIVATION));
    assistant.setAutoActivationDelay(store.getInt(PreferenceConstants.AUTO_ACTIVATION_DELAY));
    assistant.setProposalPopupOrientation(IContentAssistant.PROPOSAL_STACKED);
    assistant.setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_ABOVE);
    assistant.setContextInformationPopupBackground(SQLEditorPlugin.getDefault()
        .getSQLColorProvider().getColor(new RGB(150, 150, 0)));
    // Set auto insert mode.
    assistant.enableAutoInsert(store.getBoolean(PreferenceConstants.INSERT_SINGLE_PROPOSALS_AUTO));
    // Set to Carolina blue
    // assistant.setContextInformationPopupBackground(
    // SQLEditorPlugin.getDefault().getSQLColorProvider().getColor( new RGB( 0, 191, 255
    // )));

    return assistant;

  }

  /**
   * Creates, configures, and returns the ContentFormatter to use.
   * 
   * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getContentFormatter(ISourceViewer)
   */
  public IContentFormatter getContentFormatter(ISourceViewer sourceViewer)
  {
    ContentFormatter formatter = new ContentFormatter();
    formatter.setDocumentPartitioning(SQLPartitionScanner.SQL_PARTITIONING);

//    SQLDevToolsConfiguration factory = SQLToolsFacade
//        .getConfigurationByVendorIdentifier(getSQLEditor().getConnectionInfo()
//            .getDatabaseVendorDefinitionId());
//    IFormattingStrategy formattingStrategy = new SQLWordStrategy(factory.getSQLService()
//        .getSQLSyntax());
//    formatter.setFormattingStrategy(formattingStrategy, IDocument.DEFAULT_CONTENT_TYPE);

    return formatter;
  }

  /**
   * Gets the <code>DBProposalsService</code> object that provides content assist
   * services for this editor.
   * 
   * @return the current <code>DBProposalsService</code> object
   */
  public ISQLDBProposalsService getDBProposalsService()
  {
    return fDBProposalsService;
  }

  /**
   * Returns the double-click strategy ready to be used in this viewer when double
   * clicking onto text of the given content type. (Note: the same double-click strategy
   * object is used for all content types.)
   * 
   * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getDoubleClickStrategy(ISourceViewer,
   *      String)
   */
  public ITextDoubleClickStrategy getDoubleClickStrategy(ISourceViewer sourceViewer,
                                                         String contentType)
  {
    return new SQLDoubleClickStrategy();
  }

  /**
   * Creates, configures, and returns a presentation reconciler to help with document
   * changes.
   * 
   * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getPresentationReconciler(ISourceViewer)
   */
  public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer)
  {

    // Get the color provider.
    SQLColorProvider colorProvider = SQLEditorPlugin.getDefault().getSQLColorProvider();
    //  getSQLEditor().getSQLColorProvider();

    // Create a presentation reconciler to handle handle document changes.
    PresentationReconciler reconciler = new PresentationReconciler();
    String docPartitioning = getConfiguredDocumentPartitioning(sourceViewer);
    reconciler.setDocumentPartitioning(docPartitioning);

    // Add a "damager-repairer" for changes in default text (SQL code).
    SQLCodeScanner sqlCodeScanner = new SQLCodeScanner(SQLEditorPlugin.getDefault()
        .getSQLColorProvider());
    //SQLDevToolsConfiguration factory = SQLToolsFacade
    //    .getConfigurationByVendorIdentifier(getSQLEditor().getConnectionInfo()
    //        .getDatabaseVendorDefinitionId());
    //if(factory != null) {
    //  sqlCodeScanner.setSQLSyntax(factory.getSQLService().getSQLSyntax());
   // }
    DefaultDamagerRepairer dr = new DefaultDamagerRepairer(sqlCodeScanner);

    reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
    reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

    // rule for multiline comments
    // We just need a scanner that does nothing but returns a token with
    // the corrresponding text attributes
    dr = new DefaultDamagerRepairer(new SingleTokenScanner(new TextAttribute(colorProvider
        .getColor(SQLColorProvider.SQL_MULTILINE_COMMENT_COLOR))));
    reconciler.setDamager(dr, SQLPartitionScanner.SQL_MULTILINE_COMMENT);
    reconciler.setRepairer(dr, SQLPartitionScanner.SQL_MULTILINE_COMMENT);

    // Add a "damager-repairer" for changes witin one-line SQL comments.
    dr = new DefaultDamagerRepairer(new SingleTokenScanner(new TextAttribute(colorProvider
        .getColor(SQLColorProvider.SQL_COMMENT_COLOR))));
    reconciler.setDamager(dr, SQLPartitionScanner.SQL_COMMENT);
    reconciler.setRepairer(dr, SQLPartitionScanner.SQL_COMMENT);

    // Add a "damager-repairer" for changes witin quoted literals.
    dr = new DefaultDamagerRepairer(new SingleTokenScanner(new TextAttribute(colorProvider
        .getColor(SQLColorProvider.SQL_QUOTED_LITERAL_COLOR))));
    reconciler.setDamager(dr, SQLPartitionScanner.SQL_STRING);
    reconciler.setRepairer(dr, SQLPartitionScanner.SQL_STRING);

    // Add a "damager-repairer" for changes witin delimited identifiers.
    dr = new DefaultDamagerRepairer(new SingleTokenScanner(new TextAttribute(colorProvider
        .getColor(SQLColorProvider.SQL_DELIMITED_IDENTIFIER_COLOR))));
    reconciler.setDamager(dr, SQLPartitionScanner.SQL_DOUBLE_QUOTES_IDENTIFIER);
    reconciler.setRepairer(dr, SQLPartitionScanner.SQL_DOUBLE_QUOTES_IDENTIFIER);

    return reconciler;
  }

  /**
   * Returns the text hover which will provide the information to be shown in a text hover
   * popup window when requested for the given source viewer and the given content type.
   * 
   * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getTextHover(org.eclipse.jface.text.source.ISourceViewer,
   *      java.lang.String)
   */
  public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType)
  {
    //return new BestMatchHover(this.getSQLEditor());
    return super.getTextHover(sourceViewer, contentType);
  }

  /**
   * Sets the <code>ISQLDBProposalsService</code> object that provides content assist
   * services for this viewer to the given object.
   * 
   * @param the <code>ISQLDBProposalsService</code> object to set
   */
  public void setDBProposalsService(ISQLDBProposalsService dbProposalsService)
  {
    fDBProposalsService = dbProposalsService;
    if(fCompletionProcessor != null) {
      fCompletionProcessor.setDBProposalsService(dbProposalsService);
    }
  }

  public String[] getConfiguredContentTypes(ISourceViewer sourceViewer)
  {
    return SQLPartitionScanner.SQL_PARTITION_TYPES;
  }

  /*
   * @see SourceViewerConfiguration#getDefaultPrefixes(ISourceViewer, String)
   * @since 2.0
   */
  public String[] getDefaultPrefixes(ISourceViewer sourceViewer, String contentType)
  {
    return new String[] { "--", "" }; //$NON-NLS-1$ //$NON-NLS-2$
  }

} // end class
