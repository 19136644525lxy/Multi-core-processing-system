package com.qituo.mcps.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import com.qituo.mcps.core.MCPSMod;
import com.qituo.mcps.cloud.CloudManager;
import com.qituo.mcps.cloud.CloudBackupManager;

public class CloudServicesScreen extends Screen {
    private static final int WIDTH = 400;
    private static final int HEIGHT = 400;
    
    private ButtonWidget backupButton;
    private ButtonWidget restoreButton;
    private ButtonWidget performanceButton;
    private ButtonWidget resourceButton;
    private ButtonWidget closeButton;
    
    public CloudServicesScreen() {
        super(Text.translatable("mcps.screen.cloud_services.title"));
    }
    
    @Override
    protected void init() {
        super.init();
        
        int centerX = (this.width - WIDTH) / 2;
        int centerY = (this.height - HEIGHT) / 2;
        
        // 备份按钮
        backupButton = ButtonWidget.builder(
            Text.translatable("mcps.screen.cloud_services.backup"),
            button -> {
                CloudManager cloudManager = MCPSMod.getInstance().getCloudManager();
                if (cloudManager != null && cloudManager.isInitialized()) {
                    CloudBackupManager backupManager = cloudManager.getBackupManager();
                    if (backupManager != null) {
                        backupManager.performManualBackup();
                        this.client.inGameHud.getChatHud().addMessage(Text.translatable("mcps.screen.cloud_services.backup.created"));
                    }
                } else {
                    this.client.inGameHud.getChatHud().addMessage(Text.translatable("mcps.screen.cloud_services.not_initialized"));
                }
            }
        ).position(centerX + 50, centerY + 50).size(300, 25).build();
        this.addDrawableChild(backupButton);
        
        // 恢复按钮
        restoreButton = ButtonWidget.builder(
            Text.translatable("mcps.screen.cloud_services.restore"),
            button -> {
                CloudManager cloudManager = MCPSMod.getInstance().getCloudManager();
                if (cloudManager != null && cloudManager.isInitialized()) {
                    this.client.inGameHud.getChatHud().addMessage(Text.translatable("mcps.screen.cloud_services.restore.coming_soon"));
                } else {
                    this.client.inGameHud.getChatHud().addMessage(Text.translatable("mcps.screen.cloud_services.not_initialized"));
                }
            }
        ).position(centerX + 50, centerY + 90).size(300, 25).build();
        this.addDrawableChild(restoreButton);
        
        // 性能监控按钮
        performanceButton = ButtonWidget.builder(
            Text.translatable("mcps.screen.cloud_services.performance"),
            button -> {
                this.client.inGameHud.getChatHud().addMessage(Text.translatable("mcps.screen.cloud_services.performance.coming_soon"));
            }
        ).position(centerX + 50, centerY + 130).size(300, 25).build();
        this.addDrawableChild(performanceButton);
        
        // 资源共享按钮
        resourceButton = ButtonWidget.builder(
            Text.translatable("mcps.screen.cloud_services.resource"),
            button -> {
                this.client.inGameHud.getChatHud().addMessage(Text.translatable("mcps.screen.cloud_services.resource.coming_soon"));
            }
        ).position(centerX + 50, centerY + 170).size(300, 25).build();
        this.addDrawableChild(resourceButton);
        
        // 关闭按钮
        closeButton = ButtonWidget.builder(
            Text.translatable("mcps.screen.cloud_services.close"),
            button -> this.close()
        ).position(centerX + 50, centerY + 210).size(300, 25).build();
        this.addDrawableChild(closeButton);
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        
        int centerX = (this.width - WIDTH) / 2;
        int centerY = (this.height - HEIGHT) / 2;
        
        // 标题
        context.drawTextWithShadow(this.textRenderer, this.title, centerX + 50, centerY + 20, 0xFFFFFF);
    }
    
    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
    
    public static void open() {
        MinecraftClient.getInstance().setScreen(new CloudServicesScreen());
    }
}